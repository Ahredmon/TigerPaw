package com.tigerpaw.launcher.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import com.tigerpaw.launcher.core.data.wallpaper.WallpaperRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val wallpaperRepository: WallpaperRepository,
    private val prefs: LauncherPreferences,
) : ViewModel() {

    val darkIcons: StateFlow<Boolean> = prefs.darkIcons
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val searchIncludeApps: StateFlow<Boolean> = prefs.searchIncludeApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val searchIncludeFiles: StateFlow<Boolean> = prefs.searchIncludeFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val searchIncludeActions: StateFlow<Boolean> = prefs.searchIncludeActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

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
}
