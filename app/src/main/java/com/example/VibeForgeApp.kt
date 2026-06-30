package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.ApiKeyRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.WorkspaceRepository
import com.example.domain.usecase.AiCodingChatUseCase
import com.example.domain.usecase.ManageApiKeysUseCase
import com.example.domain.usecase.WorkspaceFileUseCase
import com.example.service.AiService
import com.example.service.FileManagerService

class VibeForgeApp : Application() {

    lateinit var database: AppDatabase
    lateinit var apiKeyRepository: ApiKeyRepository
    lateinit var chatRepository: ChatRepository
    lateinit var workspaceRepository: WorkspaceRepository
    lateinit var fileManagerService: FileManagerService
    lateinit var aiService: AiService
    
    lateinit var manageApiKeysUseCase: ManageApiKeysUseCase
    lateinit var workspaceFileUseCase: WorkspaceFileUseCase
    lateinit var aiCodingChatUseCase: AiCodingChatUseCase

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Database & Repositories
        database = AppDatabase.getDatabase(this)
        apiKeyRepository = ApiKeyRepository(database.apiKeyDao())
        chatRepository = ChatRepository(database.chatSessionDao(), database.chatMessageDao())
        workspaceRepository = WorkspaceRepository(database.pinnedFileDao(), database.snapshotDao())
        
        // Initialize Services
        fileManagerService = FileManagerService(this)
        aiService = AiService()
        
        // Initialize Use Cases
        manageApiKeysUseCase = ManageApiKeysUseCase(apiKeyRepository)
        workspaceFileUseCase = WorkspaceFileUseCase(fileManagerService, workspaceRepository)
        aiCodingChatUseCase = AiCodingChatUseCase(
            aiService = aiService,
            apiKeyRepository = apiKeyRepository,
            chatRepository = chatRepository,
            workspaceUseCase = workspaceFileUseCase,
            fileManagerService = fileManagerService
        )
    }
}
