package com.tigerpaw.launcher.feature.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.ai.AgentTool
import com.tigerpaw.launcher.core.data.ai.LocalAiRepository
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the floating assistant orb overlay.
 *
 * Manages:
 *  - speech recognition lifecycle (start / stop / results)
 *  - AI request for each recognised utterance
 *  - mic mute/unmute state
 *  - thinking state (spinning ring)
 */
@HiltViewModel
class AssistantOrbViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: LocalAiRepository,
    private val prefs: LauncherPreferences,
    tools: Set<@JvmSuppressWildcards AgentTool>,
) : ViewModel() {

    companion object {
        private const val TAG = "TigerPaw/AI/Orb"
    }

    private val toolMap: Map<String, AgentTool> = tools.associateBy { it.name }

    // ── Exposed state ─────────────────────────────────────────────────────

    private val _isMicActive = MutableStateFlow(false)
    val isMicActive: StateFlow<Boolean> = _isMicActive.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    /** Live partial / final transcript from the speech recogniser. */
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    /** Latest AI response text; blank until the first reply arrives. */
    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response.asStateFlow()

    // ── Speech recogniser ─────────────────────────────────────────────────

    private var speechRecognizer: SpeechRecognizer? = null

    /** Toggle mic. Called when the user taps the orb. */
    fun onOrbTapped() {
        if (_isMicActive.value) stopListening() else startListening()
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available on this device")
            return
        }
        releaseRecognizer()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                _isMicActive.value = true
                _transcript.value = ""
            }

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
            }

            override fun onError(error: Int) {
                Log.w(TAG, "onError code=$error")
                _isMicActive.value = false
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                Log.i(TAG, "onResults: '$text'")
                _transcript.value = text
                _isMicActive.value = false
                if (text.isNotBlank()) sendToAi(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _transcript.value = partial
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isMicActive.value = false
    }

    // ── AI call ───────────────────────────────────────────────────────────

    private fun sendToAi(text: String) {
        viewModelScope.launch {
            _isThinking.value = true
            _response.value = ""
            try {
                repository.configure(prefs.aiBaseUrl.first())
                val result = repository.chat(
                    model = prefs.aiModel.first(),
                    history = emptyList(),
                    text = text,
                    tools = toolMap,
                )
                _response.value = result.getOrNull()?.reply
                    ?: "Sorry, I couldn't process that."
                Log.i(TAG, "response='${_response.value.take(120)}'")
            } catch (e: Exception) {
                Log.e(TAG, "sendToAi failed", e)
                _response.value = "Error: ${e.message}"
            } finally {
                _isThinking.value = false
            }
        }
    }

    /** Clear transcript and response (e.g. when the overlay reopens). */
    fun reset() {
        stopListening()
        _transcript.value = ""
        _response.value = ""
        _isThinking.value = false
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    private fun releaseRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onCleared() {
        releaseRecognizer()
        super.onCleared()
    }
}
