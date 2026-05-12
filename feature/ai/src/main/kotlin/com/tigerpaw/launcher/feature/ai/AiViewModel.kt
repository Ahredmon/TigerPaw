package com.tigerpaw.launcher.feature.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.ai.AgentTool
import com.tigerpaw.launcher.core.data.ai.ApiMessage
import com.tigerpaw.launcher.core.data.ai.LocalAiRepository
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// ── UI state ─────────────────────────────────────────────────────────────────

data class UiMessage(
    val id: String = UUID.randomUUID().toString(),
    /** "user" | "assistant" | "tool_call" | "error" */
    val role: String,
    val text: String,
    /** Non-null for user messages that include an image. */
    val imageUri: Uri? = null,
    val isLoading: Boolean = false,
)

data class AiUiState(
    val messages: List<UiMessage> = emptyList(),
    val inputText: String = "",
    val pendingImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalAiRepository,
    private val prefs: LauncherPreferences,
    tools: Set<@JvmSuppressWildcards AgentTool>,
) : ViewModel() {

    companion object {
        private const val TAG = "TigerPaw/AI/VM"
    }

    /** Name → tool map for O(1) dispatch in the agentic loop. */
    private val toolMap: Map<String, AgentTool> = tools.associateBy { it.name }

    init {
        Log.d(TAG, "AiViewModel created")
    }

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    val aiEnabled: StateFlow<Boolean> = prefs.aiEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val aiBaseUrl: StateFlow<String> = prefs.aiBaseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "http://192.168.0.134:8000")

    val aiModel: StateFlow<String> = prefs.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Raw API conversation history (passed through to the repository each turn). */
    private var conversationHistory: List<ApiMessage> = emptyList()

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text, error = null) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(pendingImageUri = uri) }
    }

    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImageUri = null) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank() && state.pendingImageUri == null) return
        if (state.isProcessing) {
            Log.w(TAG, "sendMessage: already processing, ignoring")
            return
        }

        val imageUri = state.pendingImageUri
        Log.i(TAG, "sendMessage: model='${aiModel.value}' hasImage=${imageUri != null} text='${text.take(80)}'")

        // Append user message to UI
        val userMsg = UiMessage(role = "user", text = text, imageUri = imageUri)
        val thinkingMsg = UiMessage(role = "assistant", text = "", isLoading = true)
        _uiState.update { s ->
            s.copy(
                messages = s.messages + userMsg + thinkingMsg,
                inputText = "",
                pendingImageUri = null,
                isProcessing = true,
                error = null,
            )
        }

        viewModelScope.launch {
            repository.configure(aiBaseUrl.value)

            // Encode image to base-64 if present
            val base64 = imageUri?.let { ImageEncoder.encodeToBase64(it, context) }

            val result = repository.chat(
                model = aiModel.value,
                history = conversationHistory,
                text = text,
                base64Image = base64,
                tools = toolMap,
                onToolCall = { name, _ ->
                    // Show a transient "calling tool" bubble in the UI.
                    _uiState.update { s ->
                        val bubble = UiMessage(role = "tool_call", text = "⏳ $name…")
                        s.copy(messages = s.messages + bubble)
                    }
                },
            )

            result.fold(
                onSuccess = { chatResult ->
                    Log.i(TAG, "sendMessage: success reply='${chatResult.reply.take(120)}' historySize=${chatResult.messages.size}")
                    conversationHistory = chatResult.messages
                    _uiState.update { s ->
                        val reply = UiMessage(role = "assistant", text = chatResult.reply)
                        // Remove thinking bubble; remove any trailing tool_call bubbles; add reply
                        val cleaned = s.messages
                            .filter { it.role != "tool_call" }
                            .dropLastWhile { it.isLoading }
                        s.copy(messages = cleaned + reply, isProcessing = false)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "sendMessage: failed", e)
                    _uiState.update { s ->
                        val cleaned = s.messages
                            .filter { it.role != "tool_call" }
                            .dropLastWhile { it.isLoading }
                        val errMsg = UiMessage(role = "error", text = "Error: ${e.message}")
                        s.copy(messages = cleaned + errMsg, isProcessing = false, error = e.message)
                    }
                },
            )
        }
    }

    fun clearConversation() {
        Log.i(TAG, "clearConversation: was ${conversationHistory.size} messages")
        conversationHistory = emptyList()
        _uiState.update { AiUiState() }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setAiEnabled(enabled) }
    }

    fun setAiBaseUrl(url: String) {
        viewModelScope.launch { prefs.setAiBaseUrl(url) }
    }

    fun setAiModel(model: String) {
        viewModelScope.launch { prefs.setAiModel(model) }
    }
}
