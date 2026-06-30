package com.example.domain.usecase

import com.example.data.database.ApiKeyEntity
import com.example.data.repository.ApiKeyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ManageApiKeysUseCase(private val repository: ApiKeyRepository) {

    val allKeysFlow = repository.allKeysFlow

    suspend fun addKey(
        provider: String,
        name: String,
        plainKey: String,
        baseUrl: String,
        isDefault: Boolean = false
    ): Result<Long> = runCatching {
        repository.insertKey(provider, name, plainKey, baseUrl, "Active", isDefault)
    }

    suspend fun editKey(
        entity: ApiKeyEntity,
        name: String,
        plainKey: String?,
        baseUrl: String,
        isDefault: Boolean
    ): Result<Unit> = runCatching {
        val updated = entity.copy(
            name = name,
            baseUrl = baseUrl,
            isDefault = isDefault
        )
        repository.updateKey(updated, plainKey)
    }

    suspend fun deleteKey(key: ApiKeyEntity): Result<Unit> = runCatching {
        repository.deleteKey(key)
    }

    suspend fun makeDefault(id: Int, provider: String): Result<Unit> = runCatching {
        repository.setDefaultKey(id, provider)
    }

    /**
     * Performs a live connection check to see if the key is valid.
     * Updates key status in local database.
     */
    suspend fun testKeyConnection(id: Int): Result<String> = withContext(Dispatchers.IO) {
        val entity = repository.getKeyById(id) ?: return@withContext Result.failure(Exception("Key not found"))
        val plainKey = repository.getDecryptedKey(id) ?: return@withContext Result.failure(Exception("Could not decrypt key"))

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = when (entity.provider.uppercase()) {
            "GEMINI" -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://generativelanguage.googleapis.com/" else entity.baseUrl
                "${finalBaseUrl}v1beta/models?key=$plainKey"
            }
            "OPENAI" -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.openai.com/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/models" else "$finalBaseUrl/v1/models"
            }
            "GROQ" -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.groq.com/openai/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/models" else "$finalBaseUrl/v1/models"
            }
            "DEEPSEEK" -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.deepseek.com/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/models" else "$finalBaseUrl/v1/models"
            }
            "ANTHROPIC" -> {
                // Anthropic doesn't have a simple GET v1/models endpoint without heavy setup. Let's hit v1/messages with an invalid body to test authentication,
                // or simply do a test with headers.
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.anthropic.com/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/messages" else "$finalBaseUrl/v1/messages"
            }
            else -> {
                // Local Ollama
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "http://10.0.2.2:11434/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}api/tags" else "$finalBaseUrl/api/tags"
            }
        }

        val requestBuilder = Request.Builder().url(url)
        if (entity.provider.uppercase() == "OPENAI" || entity.provider.uppercase() == "GROQ" || entity.provider.uppercase() == "DEEPSEEK") {
            requestBuilder.header("Authorization", "Bearer $plainKey")
        } else if (entity.provider.uppercase() == "ANTHROPIC") {
            requestBuilder.header("x-api-key", plainKey)
            requestBuilder.header("anthropic-version", "2023-06-01")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val responseCode = response.code
            response.close()

            val success = if (entity.provider.uppercase() == "ANTHROPIC") {
                // For Anthropic, a 400 Bad Request with "x-api-key" means auth passed (an invalid key gives 401 Unauthorized)
                responseCode == 200 || responseCode == 400
            } else {
                responseCode == 200
            }

            val newStatus = if (success) "Active" else if (responseCode == 429) "Exhausted" else "Inactive"
            repository.updateKey(entity.copy(status = newStatus, lastTested = System.currentTimeMillis()))

            if (success) {
                Result.success("Connection Successful! Status: Active")
            } else {
                Result.success("Failed: Received HTTP $responseCode. Key set to $newStatus.")
            }
        } catch (e: Exception) {
            repository.updateKey(entity.copy(status = "Inactive", lastTested = System.currentTimeMillis()))
            Result.failure(e)
        }
    }
}
