package com.tigerpaw.launcher.feature.home

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.data.apps.AppRepository
import com.tigerpaw.launcher.core.data.apps.PinnedAppsRepository
import com.tigerpaw.launcher.core.data.wallpaper.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    appRepository: AppRepository,
    pinnedAppsRepository: PinnedAppsRepository,
    private val wallpaperRepository: WallpaperRepository,
) : ViewModel() {

    /** First 5 apps sorted by label — shown in the dock. */
    val dockApps: StateFlow<List<AppInfo>> = appRepository
        .getInstalledApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** User-pinned apps in their configured order. */
    val pinnedApps: StateFlow<List<AppInfo>> = pinnedAppsRepository.pinnedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _wallpaperTrigger = MutableStateFlow(0)

    val wallpaper: StateFlow<Bitmap?> = _wallpaperTrigger
        .flatMapLatest { wallpaperRepository.getCurrentWallpaper() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Re-fetches the wallpaper bitmap (call after permission is granted). */
    fun reloadWallpaper() {
        _wallpaperTrigger.value++
    }
}
