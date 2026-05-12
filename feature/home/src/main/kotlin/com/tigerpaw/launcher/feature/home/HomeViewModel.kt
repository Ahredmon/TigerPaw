package com.tigerpaw.launcher.feature.home

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.data.apps.AppRepository
import com.tigerpaw.launcher.core.data.apps.PinnedAppsRepository
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import com.tigerpaw.launcher.core.data.wallpaper.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    appRepository: AppRepository,
    pinnedAppsRepository: PinnedAppsRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val prefs: LauncherPreferences,
) : ViewModel() {

    companion object {
        private const val TAG = "TigerPaw/Home"
    }

    init {
        Log.d(TAG, "HomeViewModel created")
    }

    /** First 5 apps sorted by label — shown in the dock. */
    val dockApps: StateFlow<List<AppInfo>> = appRepository
        .getInstalledApps()
        .onEach { Log.d(TAG, "dockApps updated: ${it.size} apps") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** User-pinned apps in their configured order. */
    val pinnedApps: StateFlow<List<AppInfo>> = pinnedAppsRepository.pinnedApps
        .onEach { Log.d(TAG, "pinnedApps updated: ${it.size} pinned") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _wallpaperTrigger = MutableStateFlow(0)

    val wallpaper: StateFlow<Bitmap?> = _wallpaperTrigger
        .flatMapLatest { wallpaperRepository.getCurrentWallpaper() }
        .onEach { Log.d(TAG, "wallpaper updated: ${if (it != null) "${it.width}x${it.height}" else "null"}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Clock appearance
    val clockSize: StateFlow<String> = prefs.clockSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "medium")
    val clockWeight: StateFlow<String> = prefs.clockWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "light")
    val clockAlign: StateFlow<String> = prefs.clockAlign
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "start")
    val showBattery: StateFlow<Boolean> = prefs.showBattery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun dismissOnboarding() {
        viewModelScope.launch { prefs.setOnboardingDone() }
    }

    /** Re-fetches the wallpaper bitmap (call after permission is granted). */
    fun reloadWallpaper() {
        Log.i(TAG, "reloadWallpaper requested")
        _wallpaperTrigger.value++
    }
}
