package com.tigerpaw.launcher.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.ai.LocalAiRepository
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import com.tigerpaw.launcher.core.data.wallpaper.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val prefs: LauncherPreferences,
    private val aiRepository: LocalAiRepository,
) : ViewModel() {

    val darkIcons: StateFlow<Boolean> = prefs.darkIcons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val searchIncludeApps: StateFlow<Boolean> = prefs.searchIncludeApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val searchIncludeFiles: StateFlow<Boolean> = prefs.searchIncludeFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val searchIncludeActions: StateFlow<Boolean> = prefs.searchIncludeActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // AI Assistant
    val aiEnabled: StateFlow<Boolean> = prefs.aiEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    val aiBaseUrl: StateFlow<String> = prefs.aiBaseUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "http://192.168.0.134:8000")
    val aiModel: StateFlow<String> = prefs.aiModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _modelsLoading = MutableStateFlow(false)
    val modelsLoading: StateFlow<Boolean> = _modelsLoading.asStateFlow()

    fun fetchModels() {
        viewModelScope.launch {
            aiRepository.configure(aiBaseUrl.value)
            _modelsLoading.value = true
            aiRepository.listModels().fold(
                onSuccess = { _availableModels.value = it },
                onFailure = { /* server unreachable — keep existing list */ },
            )
            _modelsLoading.value = false
        }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch {
            wallpaperRepository.setWallpaperFromUri(uri)
            prefs.setWallpaperUri(uri.toString())
        }
    }

    fun setDarkIcons(value: Boolean) {
        viewModelScope.launch { prefs.setDarkIcons(value) }
    }

    fun setSearchIncludeApps(value: Boolean) {
        viewModelScope.launch { prefs.setSearchIncludeApps(value) }
    }

    fun setSearchIncludeFiles(value: Boolean) {
        viewModelScope.launch { prefs.setSearchIncludeFiles(value) }
    }

    fun setSearchIncludeActions(value: Boolean) {
        viewModelScope.launch { prefs.setSearchIncludeActions(value) }
    }

    fun setAiEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setAiEnabled(value) }
    }

    fun setAiBaseUrl(value: String) {
        viewModelScope.launch { prefs.setAiBaseUrl(value) }
    }

    fun setAiModel(value: String) {
        viewModelScope.launch { prefs.setAiModel(value) }
    }

    // Clock appearance
    val clockSize: StateFlow<String> = prefs.clockSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "medium")
    val clockWeight: StateFlow<String> = prefs.clockWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "light")
    val clockAlign: StateFlow<String> = prefs.clockAlign
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "start")

    fun setClockSize(value: String) {
        viewModelScope.launch { prefs.setClockSize(value) }
    }

    fun setClockWeight(value: String) {
        viewModelScope.launch { prefs.setClockWeight(value) }
    }

    fun setClockAlign(value: String) {
        viewModelScope.launch { prefs.setClockAlign(value) }
    }

    // Display
    val fullscreen: StateFlow<Boolean> = prefs.fullscreen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setFullscreen(value: Boolean) {
        viewModelScope.launch { prefs.setFullscreen(value) }
    }

    val showBattery: StateFlow<Boolean> = prefs.showBattery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setShowBattery(value: Boolean) {
        viewModelScope.launch { prefs.setShowBattery(value) }
    }

    // Shake-to-assist
    val shakeEnabled: StateFlow<Boolean> = prefs.shakeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val shakeSensitivity: StateFlow<Float> = prefs.shakeSensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.5f)

    fun setShakeEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setShakeEnabled(value) }
    }

    fun setShakeSensitivity(value: Float) {
        viewModelScope.launch { prefs.setShakeSensitivity(value) }
    }

    // Wake word
    val wakeWordEnabled: StateFlow<Boolean> = prefs.wakeWordEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setWakeWordEnabled(value: Boolean) {
        viewModelScope.launch { prefs.setWakeWordEnabled(value) }
    }
}
