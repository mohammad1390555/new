package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiKeyDao {
    @Query("SELECT * FROM api_keys ORDER BY provider ASC")
    fun getAllKeysFlow(): Flow<List<ApiKeyEntity>>

    @Query("SELECT * FROM api_keys ORDER BY provider ASC")
    suspend fun getAllKeys(): List<ApiKeyEntity>

    @Query("SELECT * FROM api_keys WHERE id = :id")
    suspend fun getKeyById(id: Int): ApiKeyEntity?

    @Query("SELECT * FROM api_keys WHERE provider = :provider")
    suspend fun getKeysByProvider(provider: String): List<ApiKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: ApiKeyEntity): Long

    @Delete
    suspend fun deleteKey(key: ApiKeyEntity)

    @Query("UPDATE api_keys SET isDefault = 0 WHERE provider = :provider")
    suspend fun clearDefaultKeysForProvider(provider: String)

    @Query("UPDATE api_keys SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultKey(id: Int)
}

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)
}

@Dao
interface PinnedFileDao {
    @Query("SELECT * FROM pinned_files")
    fun getPinnedFilesFlow(): Flow<List<PinnedFileEntity>>

    @Query("SELECT * FROM pinned_files")
    suspend fun getPinnedFiles(): List<PinnedFileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedFile(pinnedFile: PinnedFileEntity): Long

    @Query("DELETE FROM pinned_files WHERE filePath = :filePath")
    suspend fun deletePinnedFile(filePath: String)
}

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE filePath = :filePath ORDER BY timestamp DESC")
    fun getSnapshotsForFileFlow(filePath: String): Flow<List<SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SnapshotEntity): Long

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Int)
}
