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
        isDefault: Boolean = false,
        apiFormat: String = "OPENAI",
        customModel: String? = null,
        isCustomEndpoint: Boolean = false
    ): Result<Long> = runCatching {
        repository.insertKey(provider, name, plainKey, baseUrl, "Active", isDefault, apiFormat, customModel, isCustomEndpoint)
    }

    suspend fun editKey(
        entity: ApiKeyEntity,
        name: String,
        plainKey: String?,
        baseUrl: String,
        isDefault: Boolean,
        apiFormat: String = "OPENAI",
        customModel: String? = null,
        isCustomEndpoint: Boolean = false
    ): Result<Unit> = runCatching {
        val updated = entity.copy(
            name = name,
            baseUrl = baseUrl,
            isDefault = isDefault,
            apiFormat = apiFormat,
            customModel = customModel,
            isCustomEndpoint = isCustomEndpoint
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

        val isAnthropic = entity.provider.uppercase() == "ANTHROPIC" || (entity.isCustomEndpoint && entity.apiFormat.uppercase() == "ANTHROPIC")
        val isOpenAi = entity.provider.uppercase() == "OPENAI" || entity.provider.uppercase() == "GROQ" || entity.provider.uppercase() == "DEEPSEEK" || (entity.isCustomEndpoint && entity.apiFormat.uppercase() == "OPENAI")
        val isGemini = entity.provider.uppercase() == "GEMINI"

        val url = when {
            isGemini -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://generativelanguage.googleapis.com/" else entity.baseUrl
                "${finalBaseUrl}v1beta/models?key=$plainKey"
            }
            isOpenAi -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.openai.com/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/models" else "$finalBaseUrl/v1/models"
            }
            isAnthropic -> {
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "https://api.anthropic.com/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/messages" else "$finalBaseUrl/v1/messages"
            }
            else -> {
                // Local Ollama / Custom other
                val finalBaseUrl = if (entity.baseUrl.isEmpty()) "http://10.0.2.2:11434/" else entity.baseUrl
                if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}api/tags" else "$finalBaseUrl/api/tags"
            }
        }

        val requestBuilder = Request.Builder().url(url)
        if (isOpenAi) {
            if (plainKey.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $plainKey")
            }
        } else if (isAnthropic) {
            if (plainKey.isNotEmpty()) {
                requestBuilder.header("x-api-key", plainKey)
            }
            requestBuilder.header("anthropic-version", "2023-06-01")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            val responseCode = response.code
            response.close()

            val success = if (isAnthropic) {
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
