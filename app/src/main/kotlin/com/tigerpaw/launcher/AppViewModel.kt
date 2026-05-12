package com.tigerpaw.launcher

import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Activity-scoped ViewModel that carries cross-cutting navigation events.
 * Shared between [MainActivity] (producer) and [TigerPawNavHost] (consumer).
 */
@HiltViewModel
class AppViewModel @Inject constructor() : ViewModel() {

    companion object {
        private const val TAG = "TigerPaw/AppVM"
    }

    private val _openAssistant = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits [Unit] whenever the assistant screen should be opened. */
    val openAssistant = _openAssistant.asSharedFlow()

    fun requestOpenAssistant() {
        Log.i(TAG, "requestOpenAssistant")
        _openAssistant.tryEmit(Unit)
    }
}
