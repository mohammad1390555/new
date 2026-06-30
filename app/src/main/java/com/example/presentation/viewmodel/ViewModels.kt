package com.example.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.ApiKeyEntity
import com.example.data.database.ChatMessageEntity
import com.example.data.database.ChatSessionEntity
import com.example.data.database.SnapshotEntity
import com.example.data.repository.ApiKeyRepository
import com.example.domain.usecase.AiCodingChatUseCase
import com.example.domain.usecase.ManageApiKeysUseCase
import com.example.domain.usecase.WorkspaceFileUseCase
import com.example.service.FileNode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// --- API KEY VIEWMODEL ---
class ApiKeyViewModel(private val useCase: ManageApiKeysUseCase) : ViewModel() {

    val apiKeysState: StateFlow<List<ApiKeyEntity>> = useCase.allKeysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testResults = MutableStateFlow<Map<Int, String>>(emptyMap())
    val testResults: StateFlow<Map<Int, String>> = _testResults.asStateFlow()

    private val _isTesting = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val isTesting: StateFlow<Map<Int, Boolean>> = _isTesting.asStateFlow()

    fun addApiKey(provider: String, name: String, key: String, baseUrl: String, isDefault: Boolean) {
        viewModelScope.launch {
            useCase.addKey(provider, name, key, baseUrl, isDefault)
        }
    }

    fun editApiKey(entity: ApiKeyEntity, name: String, key: String?, baseUrl: String, isDefault: Boolean) {
        viewModelScope.launch {
            useCase.editKey(entity, name, key, baseUrl, isDefault)
        }
    }

    fun deleteApiKey(key: ApiKeyEntity) {
        viewModelScope.launch {
            useCase.deleteKey(key)
        }
    }

    fun testKey(id: Int) {
        viewModelScope.launch {
            _isTesting.update { it + (id to true) }
            val res = useCase.testKeyConnection(id)
            val resultMsg = res.fold(
                onSuccess = { it },
                onFailure = { it.message ?: "Connection failed" }
            )
            _testResults.update { it + (id to resultMsg) }
            _isTesting.update { it + (id to false) }
        }
    }

    fun setDefault(id: Int, provider: String) {
        viewModelScope.launch {
            useCase.makeDefault(id, provider)
        }
    }
}

// --- WORKSPACE VIEWMODEL ---
class WorkspaceViewModel(private val useCase: WorkspaceFileUseCase) : ViewModel() {

    private val _projectRootUri = MutableStateFlow<Uri?>(null)
    val projectRootUri: StateFlow<Uri?> = _projectRootUri.asStateFlow()

    private val _workspaceTree = MutableStateFlow<List<FileNode>>(emptyList())
    val workspaceTree: StateFlow<List<FileNode>> = _workspaceTree.asStateFlow()

    private val _selectedFilePath = MutableStateFlow<String?>(null)
    val selectedFilePath: StateFlow<String?> = _selectedFilePath.asStateFlow()

    private val _selectedFileContent = MutableStateFlow<String?>(null)
    val selectedFileContent: StateFlow<String?> = _selectedFileContent.asStateFlow()

    private val _snapshots = MutableStateFlow<List<SnapshotEntity>>(emptyList())
    val snapshots: StateFlow<List<SnapshotEntity>> = _snapshots.asStateFlow()

    val pinnedFiles = useCase.pinnedFilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tracks which folder paths are expanded in the UI TreeView
    val expandedFolders = mutableStateMapOf<String, Boolean>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun setProjectRoot(uri: Uri) {
        _projectRootUri.value = uri
        refreshWorkspace()
    }

    fun refreshWorkspace() {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.loadWorkspaceTree(uri)
            res.onSuccess {
                _workspaceTree.value = it
            }.onFailure {
                _workspaceTree.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun selectFile(relativePath: String) {
        _selectedFilePath.value = relativePath
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.readFile(uri, relativePath)
            res.onSuccess {
                _selectedFileContent.value = it
                loadSnapshots(relativePath)
            }.onFailure {
                _selectedFileContent.value = "Error reading file: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun closeFile() {
        _selectedFilePath.value = null
        _selectedFileContent.value = null
        _snapshots.value = emptyList()
    }

    fun saveFileContent(relativePath: String, content: String) {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.writeFile(uri, relativePath, content, autoSnapshot = true, "Manual Edit")
            res.onSuccess { success ->
                if (success) {
                    _selectedFileContent.value = content
                    loadSnapshots(relativePath)
                }
            }
            _isLoading.value = false
        }
    }

    fun createNewFile(relativePath: String, initialContent: String) {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.createNewFile(uri, relativePath, initialContent)
            res.onSuccess { success ->
                if (success) {
                    refreshWorkspace()
                }
            }
            _isLoading.value = false
        }
    }

    fun deleteFile(relativePath: String) {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.deleteFile(uri, relativePath)
            res.onSuccess { success ->
                if (success) {
                    if (_selectedFilePath.value == relativePath) {
                        closeFile()
                    }
                    refreshWorkspace()
                }
            }
            _isLoading.value = false
        }
    }

    fun togglePinFile(filePath: String, isPinned: Boolean) {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            if (isPinned) {
                useCase.unpinFile(filePath)
            } else {
                val name = filePath.split("/").last()
                useCase.pinFile(filePath, name, uri.toString())
            }
        }
    }

    private fun loadSnapshots(filePath: String) {
        viewModelScope.launch {
            useCase.getSnapshotsForFile(filePath).collect {
                _snapshots.value = it
            }
        }
    }

    fun rollback(snapshot: SnapshotEntity) {
        val uri = _projectRootUri.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = useCase.rollbackSnapshot(uri, snapshot.id, snapshot.filePath, snapshot.content)
            res.onSuccess { success ->
                if (success) {
                    _selectedFileContent.value = snapshot.content
                    loadSnapshots(snapshot.filePath)
                }
            }
            _isLoading.value = false
        }
    }

    fun toggleFolderExpanded(path: String) {
        val current = expandedFolders[path] ?: false
        expandedFolders[path] = !current
    }
}

// --- CHAT VIEWMODEL ---
class ChatViewModel(
    private val chatUseCase: AiCodingChatUseCase,
    private val apiKeyRepository: ApiKeyRepository
) : ViewModel() {

    val sessionsState: StateFlow<List<ChatSessionEntity>> = chatUseCase.sessionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _messagesState = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messagesState: StateFlow<List<ChatMessageEntity>> = _messagesState.asStateFlow()

    private val _activeProvider = MutableStateFlow("GEMINI")
    val activeProvider: StateFlow<String> = _activeProvider.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _activeStreamingResponse = MutableStateFlow("")
    val activeStreamingResponse: StateFlow<String> = _activeStreamingResponse.asStateFlow()

    init {
        // Automatically create or load first session
        viewModelScope.launch {
            sessionsState.collect { sessions ->
                if (sessions.isNotEmpty() && _currentSessionId.value == null) {
                    selectSession(sessions.first().id)
                } else if (sessions.isEmpty() && _currentSessionId.value == null) {
                    val defaultId = UUID.randomUUID().toString()
                    chatUseCase.createNewSession(defaultId, "Coding Workspace")
                    selectSession(defaultId)
                }
            }
        }
    }

    fun selectSession(id: String) {
        _currentSessionId.value = id
        viewModelScope.launch {
            chatUseCase.getMessagesForSession(id).collect {
                _messagesState.value = it
            }
        }
    }

    fun createNewSession(title: String) {
        val id = UUID.randomUUID().toString()
        viewModelScope.launch {
            chatUseCase.createNewSession(id, title)
            selectSession(id)
        }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch {
            chatUseCase.deleteSession(id)
            if (_currentSessionId.value == id) {
                _currentSessionId.value = null
                _messagesState.value = emptyList()
            }
        }
    }

    fun setProvider(provider: String) {
        _activeProvider.value = provider
    }

    fun clearMessages() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            chatUseCase.clearMessagesForSession(sessionId)
        }
    }

    fun sendMessage(prompt: String, projectRootUri: Uri?) {
        val sessionId = _currentSessionId.value ?: return
        if (prompt.trim().isEmpty()) return

        viewModelScope.launch {
            _isGenerating.value = true
            _activeStreamingResponse.value = ""
            
            // 1. Save user query locally
            chatUseCase.saveUserMessage(sessionId, prompt)

            // 2. Stream AI Response and handle tool execution loops
            chatUseCase.executeStreamingQuery(
                sessionId = sessionId,
                provider = _activeProvider.value,
                prompt = prompt,
                projectRootUri = projectRootUri,
                onResponseChunk = { chunk ->
                    _activeStreamingResponse.value += chunk
                },
                onToolCallExecuted = { name, args, result ->
                    // Logs tool executions visually into streaming view if desired
                },
                onError = { error ->
                    _activeStreamingResponse.value += "\n\n⚠️ Error: $error"
                }
            )

            _isGenerating.value = false
            _activeStreamingResponse.value = ""
        }
    }
}

// --- DEPENDENCY FACTORY ---
class ViewModelFactory(
    private val manageApiKeysUseCase: ManageApiKeysUseCase,
    private val workspaceUseCase: WorkspaceFileUseCase,
    private val chatUseCase: AiCodingChatUseCase,
    private val apiKeyRepository: ApiKeyRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ApiKeyViewModel::class.java) -> {
                ApiKeyViewModel(manageApiKeysUseCase) as T
            }
            modelClass.isAssignableFrom(WorkspaceViewModel::class.java) -> {
                WorkspaceViewModel(workspaceUseCase) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(chatUseCase, apiKeyRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
