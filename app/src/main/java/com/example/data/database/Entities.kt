package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_keys")
data class ApiKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val provider: String, // GEMINI, OPENAI, CLAUDE, GROQ, DEEPSEEK, CUSTOM
    val name: String,
    val encryptedKey: String,
    val baseUrl: String,
    val status: String, // Active, Inactive, Exhausted
    val lastTested: Long = 0L,
    val isDefault: Boolean = false,
    val apiFormat: String = "OPENAI", // OPENAI or ANTHROPIC
    val customModel: String? = null,
    val isCustomEndpoint: Boolean = false
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val role: String, // user, assistant, system, tool
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCallId: String? = null
)

@Entity(tableName = "pinned_files")
data class PinnedFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String, // Relative path inside workspace
    val displayName: String,
    val projectUri: String // Root directory URI (SAF)
)

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String, // Relative path inside workspace
    val content: String, // Stored content to easily roll back
    val timestamp: Long = System.currentTimeMillis(),
    val description: String
)
