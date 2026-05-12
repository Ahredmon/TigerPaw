package com.tigerpaw.launcher.core.data.ai

import android.util.Log
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAiRepository @Inject constructor() {
    companion object {
        private const val TAG = "TigerPaw/AI/Repo"
        /** Safety cap on the tool-call → response loop to prevent infinite cycles. */
        private const val MAX_TOOL_ROUNDS = 8
    }

    @Volatile
    private var api: LocalAiApi? = null

    @Volatile
    private var currentBaseUrl: String = ""

    fun configure(baseUrl: String) {
        if (baseUrl != currentBaseUrl) {
            Log.i(TAG, "configure: new baseUrl='$baseUrl' (was='$currentBaseUrl')")
            api = LocalAiApi(baseUrl)
            currentBaseUrl = baseUrl
        }
    }

    private fun requireApi(): LocalAiApi =
        api ?: throw IllegalStateException("LocalAiRepository not configured. Call configure() first.")

    suspend fun listModels(): Result<List<String>> {
        Log.d(TAG, "listModels()")
        return requireApi().listModels()
    }

    /**
     * Single-turn or agentic multi-turn chat.
     *
     * When [tools] is non-empty the model may respond with `finish_reason = "tool_calls"`.
     * In that case every requested tool is executed and its result fed back into the conversation;
     * this repeats up to [MAX_TOOL_ROUNDS] times before returning whatever text the model has
     * produced so far.
     *
     * @param tools       Map of tool name → [AgentTool]. Empty = no tool calling.
     * @param onToolCall  Optional callback invoked just before each tool execution
     *                    (useful for showing progress in the UI).
     */
    suspend fun chat(
        model: String,
        history: List<ApiMessage>,
        text: String,
        base64Image: String? = null,
        imageMimeType: String = "image/jpeg",
        tools: Map<String, AgentTool> = emptyMap(),
        onToolCall: (suspend (name: String, args: String) -> Unit)? = null,
    ): Result<AiChatResult> {
        val api = requireApi()
        Log.i(TAG, "chat: model='$model' historySize=${history.size} tools=${tools.keys} hasImage=${base64Image != null} text='${text.take(80)}'")

        val userMessage = if (base64Image != null) {
            LocalAiApi.visionMessage(text, base64Image, imageMimeType)
        } else {
            LocalAiApi.textMessage("user", text)
        }

        val messages = (history + userMessage).toMutableList()

        // Build tool definitions for the request (if any tools are provided).
        val toolDefs = if (tools.isNotEmpty()) {
            tools.values.map { tool ->
                ToolDefinition(
                    function = FunctionDefinition(
                        name = tool.name,
                        description = tool.description,
                        parameters = parseParametersSchema(tool.parametersSchema),
                    ),
                )
            }
        } else null

        var round = 0
        while (true) {
            val request = ChatCompletionRequest(
                model = model,
                messages = messages,
                tools = toolDefs,
                toolChoice = if (toolDefs != null) "auto" else null,
            )

            val response = api.chatCompletion(request)
            if (response.isFailure) {
                Log.e(TAG, "chat: chatCompletion failed (round=$round)", response.exceptionOrNull())
                return Result.failure(response.exceptionOrNull()!!)
            }

            val choice = response.getOrThrow().choices.firstOrNull()
                ?: return Result.failure(IllegalStateException("No choices in response"))

            val assistantMsg = choice.message
            messages.add(assistantMsg)

            val finishReason = choice.finishReason
            Log.d(TAG, "chat: round=$round finishReason='$finishReason' toolCalls=${assistantMsg.toolCalls?.size ?: 0}")

            // ── Normal text response ──────────────────────────────────────────
            if (finishReason != "tool_calls" || assistantMsg.toolCalls.isNullOrEmpty()) {
                val replyText = when (val c = assistantMsg.content) {
                    is JsonPrimitive -> c.content
                    else -> c?.toString() ?: ""
                }
                Log.i(TAG, "chat: done after $round tool round(s). reply='${replyText.take(120)}'")
                return Result.success(AiChatResult(messages = messages, reply = replyText))
            }

            // ── Tool call round ───────────────────────────────────────────────
            if (++round > MAX_TOOL_ROUNDS) {
                Log.w(TAG, "chat: hit MAX_TOOL_ROUNDS=$MAX_TOOL_ROUNDS, stopping")
                return Result.success(AiChatResult(messages = messages, reply = "[Tool call limit reached]"))
            }

            for (toolCall in assistantMsg.toolCalls!!) {
                val toolName = toolCall.function.name
                val args = toolCall.function.arguments
                val tool = tools[toolName]

                Log.i(TAG, "chat: executing tool='$toolName' id='${toolCall.id}' args='${args.take(200)}'")
                onToolCall?.invoke(toolName, args)

                val result = if (tool != null) {
                    try {
                        tool.execute(args)
                    } catch (e: Exception) {
                        Log.e(TAG, "chat: tool '$toolName' threw", e)
                        "Error: ${e.message}"
                    }
                } else {
                    Log.w(TAG, "chat: unknown tool '$toolName'")
                    "Error: tool '$toolName' is not available."
                }

                Log.d(TAG, "chat: tool='$toolName' result='${result.take(200)}'")
                messages.add(LocalAiApi.toolResultMessage(toolCallId = toolCall.id, result = result))
            }
            // Continue the loop — send tool results back to the model.
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parse the [AgentTool.parametersSchema] JSON string into a [JsonObject].
     * Falls back to an empty schema on parse failure so a bad tool definition never
     * crashes the whole request.
     */
    private fun parseParametersSchema(schema: String): JsonObject {
        return try {
            kotlinx.serialization.json.Json.parseToJsonElement(schema) as? JsonObject
                ?: buildJsonObject { put("type", "object") }
        } catch (e: Exception) {
            Log.w(TAG, "parseParametersSchema: failed to parse schema", e)
            buildJsonObject { put("type", "object") }
        }
    }
}

data class AiChatResult(
    val messages: List<ApiMessage>,
    val reply: String,
)

