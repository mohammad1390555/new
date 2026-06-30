package com.example.service

import com.example.data.database.ApiKeyEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class ChatRole {
    object User : ChatRole()
    object Assistant : ChatRole()
    object System : ChatRole()
    data class Tool(val name: String, val callId: String) : ChatRole()
}

data class ChatMessage(
    val role: ChatRole,
    val content: String,
    val toolCallId: String? = null
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

sealed class AiResponseChunk {
    data class Content(val text: String) : AiResponseChunk()
    data class FunctionCallRequest(val toolCalls: List<ToolCall>) : AiResponseChunk()
    data class Error(val message: String) : AiResponseChunk()
}

class AiService {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaTypeJson = "application/json".toMediaType()

    /**
     * Streams response from Gemini or OpenAI compatible providers.
     * Implements intelligent rotation on rate limits (429).
     */
    fun generateCodingResponseStream(
        providerKeys: List<ApiKeyEntity>,
        provider: String,
        messages: List<ChatMessage>,
        systemInstruction: String,
        tools: List<ToolDefinition> = emptyList(),
        customBaseUrl: String? = null,
        customModel: String? = null
    ): Flow<AiResponseChunk> = flow {
        if (providerKeys.isEmpty()) {
            emit(AiResponseChunk.Error("No active API keys found for $provider. Please configure keys in Settings."))
            return@flow
        }

        // Rotate keys in case of rate limits
        var keyIndex = 0
        var success = false
        var lastErrorMsg = "Unknown error"

        while (keyIndex < providerKeys.size && !success) {
            val apiKeyEntity = providerKeys[keyIndex]
            val apiKey = com.example.data.security.CryptoHelper.decrypt(apiKeyEntity.encryptedKey)

            try {
                val flowToCollect = when (provider.uppercase()) {
                    "GEMINI" -> streamGemini(apiKey, messages, systemInstruction, tools, apiKeyEntity.baseUrl, customModel)
                    else -> streamOpenAiCompatible(apiKey, provider, messages, systemInstruction, tools, customBaseUrl ?: apiKeyEntity.baseUrl, customModel)
                }
                
                var receivedChunks = false
                flowToCollect.collect { chunk ->
                    receivedChunks = true
                    if (chunk is AiResponseChunk.Error && chunk.message.contains("429")) {
                        throw RateLimitException(chunk.message)
                    }
                    emit(chunk)
                }
                if (receivedChunks) {
                    success = true
                }
            } catch (e: RateLimitException) {
                keyIndex++
                lastErrorMsg = "Key $keyIndex rate limited (429). Rotating..."
                emit(AiResponseChunk.Content("\n*[Rate-limited. Rotating to key #${keyIndex + 1}...]*\n"))
            } catch (e: Exception) {
                lastErrorMsg = e.message ?: "Network error"
                keyIndex++
            }
        }

        if (!success) {
            emit(AiResponseChunk.Error("All rotated keys failed. Last error: $lastErrorMsg"))
        }
    }.flowOn(Dispatchers.IO)

    private class RateLimitException(message: String) : Exception(message)

    /**
     * Gemini Direct REST Stream
     */
    private fun streamGemini(
        apiKey: String,
        messages: List<ChatMessage>,
        systemInstruction: String,
        tools: List<ToolDefinition>,
        baseUrl: String,
        customModel: String?
    ): Flow<AiResponseChunk> = flow {
        val finalBaseUrl = if (baseUrl.isEmpty()) "https://generativelanguage.googleapis.com/" else baseUrl
        val model = customModel ?: "gemini-3.5-flash"
        val url = "${finalBaseUrl}v1beta/models/$model:streamGenerateContent?key=$apiKey"

        // Map messages to Gemini REST format
        val contentsList = messages.map { msg ->
            val roleStr = when (msg.role) {
                ChatRole.User -> "user"
                ChatRole.Assistant -> "model"
                else -> "user"
            }
            mapOf(
                "role" to roleStr,
                "parts" to listOf(mapOf("text" to msg.content))
            )
        }

        val requestBodyMap = mutableMapOf<String, Any>(
            "contents" to contentsList
        )

        if (systemInstruction.isNotEmpty()) {
            requestBodyMap["systemInstruction"] = mapOf(
                "parts" to listOf(mapOf("text" to systemInstruction))
            )
        }

        // Add function calling tools
        if (tools.isNotEmpty()) {
            val declarations = tools.map { t ->
                val parametersMap = mutableMapOf<String, Any>(
                    "type" to "OBJECT",
                    "properties" to t.parameters.mapValues { (_, value) ->
                        if (value is Map<*, *>) value else mapOf("type" to value.toString())
                    }
                )
                if (t.parameters.keys.isNotEmpty()) {
                    parametersMap["required"] = t.parameters.keys.toList()
                }

                mapOf(
                    "name" to t.name,
                    "description" to t.description,
                    "parameters" to parametersMap
                )
            }
            requestBodyMap["tools"] = listOf(mapOf("functionDeclarations" to declarations))
        }

        val jsonStr = moshi.adapter(Map::class.java).toJson(requestBodyMap)
        val request = Request.Builder()
            .url(url)
            .post(jsonStr.toRequestBody(mediaTypeJson))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(AiResponseChunk.Error("Gemini Error: Code ${response.code}. Msg: ${response.message}"))
                return@flow
            }
            
            val reader = BufferedReader(InputStreamReader(response.body?.byteStream(), Charsets.UTF_8))
            var line: String?
            val stringBuilder = StringBuilder()

            // Gemini streaming outputs a JSON array: [ { "candidates": [...] }, ... ]
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty() || trimmed == "[" || trimmed == "]") continue
                
                val cleanLine = if (trimmed.startsWith(",")) trimmed.substring(1) else trimmed
                try {
                    val map = moshi.adapter(Map::class.java).fromJson(cleanLine)
                    val candidates = map?.get("candidates") as? List<*>
                    val candidate = candidates?.firstOrNull() as? Map<*, *>
                    val content = candidate?.get("content") as? Map<*, *>
                    val parts = content?.get("parts") as? List<*>
                    val part = parts?.firstOrNull() as? Map<*, *>
                    
                    // Check for function call
                    val funcCall = part?.get("functionCall") as? Map<*, *>
                    if (funcCall != null) {
                        val name = funcCall["name"] as? String ?: ""
                        val argsMap = funcCall["args"] as? Map<*, *>
                        val argsJson = if (argsMap != null) moshi.adapter(Map::class.java).toJson(argsMap) else "{}"
                        emit(AiResponseChunk.FunctionCallRequest(listOf(ToolCall("gemini_call", name, argsJson))))
                    } else {
                        val text = part?.get("text") as? String
                        if (text != null) {
                            emit(AiResponseChunk.Content(text))
                        }
                    }
                } catch (e: Exception) {
                    // Accumulate or ignore parsing errors due to partial JSON chunks
                }
            }
        }
    }

    /**
     * OpenAI/Anthropic/Groq/DeepSeek/Ollama/LM Studio SSE stream
     */
    private fun streamOpenAiCompatible(
        apiKey: String,
        provider: String,
        messages: List<ChatMessage>,
        systemInstruction: String,
        tools: List<ToolDefinition>,
        baseUrl: String,
        customModel: String?
    ): Flow<AiResponseChunk> = flow {
        val finalBaseUrl = if (baseUrl.isEmpty()) {
            when (provider.uppercase()) {
                "OPENAI" -> "https://api.openai.com/"
                "GROQ" -> "https://api.groq.com/openai/"
                "DEEPSEEK" -> "https://api.deepseek.com/"
                "ANTHROPIC" -> "https://api.anthropic.com/"
                else -> "http://10.0.2.2:11434/" // local computer Ollama via android emulator localhost
            }
        } else baseUrl

        val model = customModel ?: when (provider.uppercase()) {
            "OPENAI" -> "gpt-4o-mini"
            "GROQ" -> "llama-3.3-70b-versatile"
            "DEEPSEEK" -> "deepseek-coder"
            "ANTHROPIC" -> "claude-3-5-sonnet-latest"
            else -> "llama3"
        }

        val url = if (finalBaseUrl.endsWith("/")) "${finalBaseUrl}v1/chat/completions" else "$finalBaseUrl/v1/chat/completions"

        // Standard OpenAI request payload
        val openAiMessages = mutableListOf<Map<String, Any>>()
        
        if (systemInstruction.isNotEmpty()) {
            openAiMessages.add(mapOf("role" to "system", "content" to systemInstruction))
        }

        messages.forEach { msg ->
            val roleStr = when (msg.role) {
                ChatRole.User -> "user"
                ChatRole.Assistant -> "assistant"
                ChatRole.System -> "system"
                is ChatRole.Tool -> "tool"
            }
            if (msg.role is ChatRole.Tool) {
                openAiMessages.add(mapOf(
                    "role" to roleStr,
                    "tool_call_id" to msg.role.callId,
                    "name" to msg.role.name,
                    "content" to msg.content
                ))
            } else {
                openAiMessages.add(mapOf("role" to roleStr, "content" to msg.content))
            }
        }

        val requestBodyMap = mutableMapOf<String, Any>(
            "model" to model,
            "messages" to openAiMessages,
            "stream" to true
        )

        // Add tools for OpenAI compatible providers if they support it
        if (tools.isNotEmpty() && provider.uppercase() != "ANTHROPIC") {
            val toolsList = tools.map { t ->
                mapOf(
                    "type" to "function",
                    "function" to mapOf(
                        "name" to t.name,
                        "description" to t.description,
                        "parameters" to mapOf(
                            "type" to "OBJECT",
                            "properties" to t.parameters,
                            "required" to t.parameters.keys.toList()
                        )
                    )
                )
            }
            requestBodyMap["tools"] = toolsList
        }

        val jsonStr = moshi.adapter(Map::class.java).toJson(requestBodyMap)
        val requestBuilder = Request.Builder()
            .url(url)
            .post(jsonStr.toRequestBody(mediaTypeJson))

        if (apiKey.isNotEmpty()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        // Anthropic special headers if bypassed via custom gateway or standard proxy
        if (provider.uppercase() == "ANTHROPIC") {
            requestBuilder.header("x-api-key", apiKey)
            requestBuilder.header("anthropic-version", "2023-06-01")
        }

        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit(AiResponseChunk.Error("$provider Error: Code ${response.code}. Msg: ${response.message}"))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(response.body?.byteStream(), Charsets.UTF_8))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (!trimmed.startsWith("data: ")) continue
                val data = trimmed.removePrefix("data: ").trim()
                if (data == "[DONE]") break

                try {
                    val chunkMap = moshi.adapter(Map::class.java).fromJson(data)
                    val choices = chunkMap?.get("choices") as? List<*>
                    val choice = choices?.firstOrNull() as? Map<*, *>
                    
                    // Check for tool calls (OpenAI style)
                    val delta = choice?.get("delta") as? Map<*, *>
                    val toolCallsList = delta?.get("tool_calls") as? List<*>
                    if (toolCallsList != null && toolCallsList.isNotEmpty()) {
                        val toolCalls = toolCallsList.mapNotNull { tc ->
                            val tcMap = tc as? Map<*, *>
                            val id = tcMap?.get("id") as? String ?: "call_id"
                            val function = tcMap?.get("function") as? Map<*, *>
                            val name = function?.get("name") as? String ?: ""
                            val args = function?.get("arguments") as? String ?: ""
                            if (name.isNotEmpty()) ToolCall(id, name, args) else null
                        }
                        if (toolCalls.isNotEmpty()) {
                            emit(AiResponseChunk.FunctionCallRequest(toolCalls))
                        }
                    } else {
                        val text = delta?.get("content") as? String
                        if (text != null) {
                            emit(AiResponseChunk.Content(text))
                        }
                    }
                } catch (e: Exception) {
                    // Ignore decoding failures of partial SSE chunks
                }
            }
        }
    }
}
