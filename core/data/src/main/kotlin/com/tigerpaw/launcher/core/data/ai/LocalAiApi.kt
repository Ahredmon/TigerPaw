package com.tigerpaw.launcher.core.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Thin HTTP wrapper around the LocalAI OpenAI-compatible `/v1/chat/completions` endpoint.
 *
 * This class is intentionally stateless; conversation history management lives in
 * [LocalAiRepository].
 */
class LocalAiApi(baseUrl: String) {

    companion object {
        private const val TAG = "TigerPaw/AI/Api"

        /** Build a text-only [ApiMessage]. */
        fun textMessage(role: String, text: String): ApiMessage =
            ApiMessage(role = role, content = JsonPrimitive(text))

        /**
         * Build a tool-result [ApiMessage] (role = "tool") to feed back into the conversation
         * after executing a tool call.
         */
        fun toolResultMessage(toolCallId: String, result: String): ApiMessage =
            ApiMessage(
                role = "tool",
                content = JsonPrimitive(result),
                toolCallId = toolCallId,
            )

        /** Build a user [ApiMessage] that includes an image as a base-64 data URL. */
        fun visionMessage(text: String, base64Image: String, mimeType: String = "image/jpeg"): ApiMessage {
            val parts = JsonArray(
                listOf(
                    Json.encodeToJsonElement(
                        ContentPart.serializer(),
                        ContentPart(type = "text", text = text)
                    ),
                    Json.encodeToJsonElement(
                        ContentPart.serializer(),
                        ContentPart(
                            type = "image_url",
                            imageUrl = ImageUrlContent("data:$mimeType;base64,$base64Image")
                        )
                    ),
                )
            )
            return ApiMessage(role = "user", content = parts)
        }

    }

    private val completionsUrl = baseUrl.trimEnd('/') + "/v1/chat/completions"
    private val modelsUrl = baseUrl.trimEnd('/') + "/v1/models"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chatCompletion(request: ChatCompletionRequest): Result<ChatCompletionResponse> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "chatCompletion → model='${request.model}' messages=${request.messages.size}")
            try {
                val body = json.encodeToString(request).toRequestBody(jsonMediaType)
                val httpRequest = Request.Builder()
                    .url(completionsUrl)
                    .post(body)
                    .build()

                val response = client.newCall(httpRequest).execute()
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IllegalStateException("Empty response body"))

                if (!response.isSuccessful) {
                    Log.e(TAG, "chatCompletion HTTP ${response.code}: $responseBody")
                    return@withContext Result.failure(
                        IllegalStateException("HTTP ${response.code}: $responseBody")
                    )
                }

                val parsed = json.decodeFromString<ChatCompletionResponse>(responseBody)
                val finishReason = parsed.choices.firstOrNull()?.finishReason
                Log.d(TAG, "chatCompletion ← finish='$finishReason' bodyBytes=${responseBody.length}")
                Result.success(parsed)
            } catch (e: Exception) {
                Log.e(TAG, "chatCompletion exception", e)
                Result.failure(e)
            }
        }

    suspend fun listModels(): Result<List<String>> =
        withContext(Dispatchers.IO) {
            Log.d(TAG, "listModels → $modelsUrl")
            try {
                val request = Request.Builder().url(modelsUrl).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                    ?: return@withContext Result.failure(IllegalStateException("Empty response body"))
                if (!response.isSuccessful) {
                    Log.e(TAG, "listModels HTTP ${response.code}: $body")
                    return@withContext Result.failure(IllegalStateException("HTTP ${response.code}: $body"))
                }
                val parsed = json.decodeFromString<ModelsListResponse>(body)
                val models = parsed.data.map { it.id }.sorted()
                Log.i(TAG, "listModels ← ${models.size} models: $models")
                Result.success(models)
            } catch (e: Exception) {
                Log.e(TAG, "listModels exception", e)
                Result.failure(e)
            }
        }
}
