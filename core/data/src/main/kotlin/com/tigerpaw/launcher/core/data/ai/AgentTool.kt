package com.tigerpaw.launcher.core.data.ai

/**
 * Defines a capability the AI agent can invoke during its reasoning loop.
 *
 * Implement this interface in the feature layer (where Android context is available)
 * and inject instances into [LocalAiRepository] via Hilt multibindings.
 */
interface AgentTool {
    /** Unique name the model will use in `tool_calls`. Must be snake_case, ≤64 chars. */
    val name: String

    /** Human-readable description sent to the model to explain when to use this tool. */
    val description: String

    /**
     * JSON Schema object describing the tool's parameters.
     * Example:
     * ```json
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": { "type": "string", "description": "Search query" }
     *   },
     *   "required": ["query"]
     * }
     * ```
     */
    val parametersSchema: String

    /**
     * Execute the tool with the given arguments JSON string (as supplied by the model).
     * Returns a plain-text result that will be fed back to the model as a tool message.
     * Should not throw — catch errors and return a descriptive error string instead.
     */
    suspend fun execute(argumentsJson: String): String
}
