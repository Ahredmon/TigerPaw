package com.tigerpaw.launcher.core.data.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ── Request models ────────────────────────────────────────────────────────────

/**
 * POST /v1/chat/completions request body, aligned with the llama-openai-server contract.
 * Roles accepted by the server: "system" | "user" | "assistant".
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ApiMessage>,
    val stream: Boolean = false,
    val temperature: Float? = null,
    @SerialName("top_p") val topP: Float? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("frequency_penalty") val frequencyPenalty: Float? = null,
    @SerialName("presence_penalty") val presencePenalty: Float? = null,
    val stop: List<String>? = null,
    val seed: Int? = null,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    /** Tool definitions exposed to the model. Null → no tool calling. */
    val tools: List<ToolDefinition>? = null,
    /** "auto" | "none" | "required" — controls whether/when the model calls tools. */
    @SerialName("tool_choice") val toolChoice: String? = null,
)

@Serializable
data class ResponseFormat(
    /** "text" or "json_object" */
    val type: String = "text",
)

/**
 * A message in the conversation.
 * [content] is [JsonElement] because it can be:
 *   - a [kotlinx.serialization.json.JsonPrimitive] (plain text)
 *   - a [kotlinx.serialization.json.JsonArray] of [ContentPart]s (multimodal)
 */
@Serializable
data class ApiMessage(
    /** One of: "system", "user", "assistant", "tool" */
    val role: String,
    val content: JsonElement? = null,
    /** Populated by the model when it wants to call one or more tools. */
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    /** Required on "tool" role messages — matches the [ToolCall.id] being answered. */
    @SerialName("tool_call_id") val toolCallId: String? = null,
)

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrlContent? = null,
)

@Serializable
data class ImageUrlContent(
    val url: String, // "data:image/jpeg;base64,..." or http URL
)

// ── Tool-calling models ───────────────────────────────────────────────────────

/**
 * Describes a callable function to the model.
 * Sent in [ChatCompletionRequest.tools].
 */
@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: FunctionDefinition,
)

@Serializable
data class FunctionDefinition(
    val name: String,
    val description: String,
    /** Raw JSON Schema object describing accepted parameters. */
    val parameters: JsonObject,
)

/**
 * A single tool-call emitted by the model inside an assistant message.
 */
@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCallPayload,
)

/**
 * The model's chosen function name + serialised JSON arguments string.
 */
@Serializable
data class FunctionCallPayload(
    val name: String,
    /** Raw JSON string — parse with `JSONObject(arguments)`. */
    val arguments: String,
)

// ── Response models ───────────────────────────────────────────────────────────

@Serializable
data class ChatCompletionResponse(
    val id: String = "",
    val model: String = "",
    val created: Long = 0L,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null,
)

@Serializable
data class Choice(
    val message: ApiMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
    val index: Int = 0,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

// ── Models list response ──────────────────────────────────────────────────────

@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList(),
)

@Serializable
data class ModelInfo(
    val id: String,
)
