package com.example.data.repository

import com.example.data.database.*
import com.example.data.security.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ApiKeyRepository(private val apiKeyDao: ApiKeyDao) {
    val allKeysFlow: Flow<List<ApiKeyEntity>> = apiKeyDao.getAllKeysFlow()

    suspend fun getAllKeys(): List<ApiKeyEntity> = withContext(Dispatchers.IO) {
        apiKeyDao.getAllKeys()
    }

    suspend fun getKeyById(id: Int): ApiKeyEntity? = withContext(Dispatchers.IO) {
        apiKeyDao.getKeyById(id)
    }

    suspend fun insertKey(
        provider: String,
        name: String,
        plainKey: String,
        baseUrl: String,
        status: String,
        isDefault: Boolean = false
    ): Long = withContext(Dispatchers.IO) {
        val encrypted = CryptoHelper.encrypt(plainKey)
        val entity = ApiKeyEntity(
            provider = provider,
            name = name,
            encryptedKey = encrypted,
            baseUrl = baseUrl,
            status = status,
            isDefault = isDefault
        )
        if (isDefault) {
            apiKeyDao.clearDefaultKeysForProvider(provider)
        }
        apiKeyDao.insertKey(entity)
    }

    suspend fun updateKey(entity: ApiKeyEntity, plainKey: String? = null) = withContext(Dispatchers.IO) {
        val finalEntity = if (plainKey != null) {
            entity.copy(encryptedKey = CryptoHelper.encrypt(plainKey))
        } else {
            entity
        }
        if (finalEntity.isDefault) {
            apiKeyDao.clearDefaultKeysForProvider(finalEntity.provider)
        }
        apiKeyDao.insertKey(finalEntity)
    }

    suspend fun deleteKey(key: ApiKeyEntity) = withContext(Dispatchers.IO) {
        apiKeyDao.deleteKey(key)
    }

    suspend fun getDecryptedKey(id: Int): String? = withContext(Dispatchers.IO) {
        val entity = apiKeyDao.getKeyById(id) ?: return@withContext null
        CryptoHelper.decrypt(entity.encryptedKey)
    }

    suspend fun getDecryptedKeyForProvider(provider: String): String? = withContext(Dispatchers.IO) {
        val keys = apiKeyDao.getKeysByProvider(provider)
        // Find default or first active key
        val target = keys.find { it.isDefault && it.status == "Active" } 
            ?: keys.find { it.status == "Active" }
            ?: keys.firstOrNull()
        target?.let { CryptoHelper.decrypt(it.encryptedKey) }
    }

    suspend fun getActiveKeysForProvider(provider: String): List<ApiKeyEntity> = withContext(Dispatchers.IO) {
        apiKeyDao.getKeysByProvider(provider).filter { it.status == "Active" }
    }

    suspend fun setDefaultKey(id: Int, provider: String) = withContext(Dispatchers.IO) {
        apiKeyDao.clearDefaultKeysForProvider(provider)
        apiKeyDao.setDefaultKey(id)
    }
}

class ChatRepository(
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatSessionDao.getAllSessionsFlow()

    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>> {
        return chatMessageDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatMessageDao.getMessagesForSession(sessionId)
    }

    suspend fun createSession(id: String, title: String) = withContext(Dispatchers.IO) {
        chatSessionDao.insertSession(ChatSessionEntity(id = id, title = title))
    }

    suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
        chatSessionDao.deleteSessionById(id)
        chatMessageDao.deleteMessagesForSession(id)
    }

    suspend fun deleteMessagesForSession(sessionId: String) = withContext(Dispatchers.IO) {
        chatMessageDao.deleteMessagesForSession(sessionId)
    }

    suspend fun insertMessage(
        sessionId: String,
        role: String,
        content: String,
        toolCallId: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val entity = ChatMessageEntity(
            sessionId = sessionId,
            role = role,
            content = content,
            toolCallId = toolCallId
        )
        chatMessageDao.insertMessage(entity)
    }
}

class WorkspaceRepository(
    private val pinnedFileDao: PinnedFileDao,
    private val snapshotDao: SnapshotDao
) {
    val pinnedFilesFlow: Flow<List<PinnedFileEntity>> = pinnedFileDao.getPinnedFilesFlow()

    suspend fun getPinnedFiles(): List<PinnedFileEntity> = withContext(Dispatchers.IO) {
        pinnedFileDao.getPinnedFiles()
    }

    suspend fun pinFile(filePath: String, displayName: String, projectUri: String) = withContext(Dispatchers.IO) {
        pinnedFileDao.insertPinnedFile(
            PinnedFileEntity(
                filePath = filePath,
                displayName = displayName,
                projectUri = projectUri
            )
        )
    }

    suspend fun unpinFile(filePath: String) = withContext(Dispatchers.IO) {
        pinnedFileDao.deletePinnedFile(filePath)
    }

    fun getSnapshotsFlow(filePath: String): Flow<List<SnapshotEntity>> {
        return snapshotDao.getSnapshotsForFileFlow(filePath)
    }

    suspend fun saveSnapshot(filePath: String, content: String, description: String) = withContext(Dispatchers.IO) {
        snapshotDao.insertSnapshot(
            SnapshotEntity(
                filePath = filePath,
                content = content,
                description = description
            )
        )
    }

    suspend fun deleteSnapshot(id: Int) = withContext(Dispatchers.IO) {
        snapshotDao.deleteSnapshotById(id)
    }
}
