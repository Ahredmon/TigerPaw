package com.tigerpaw.launcher.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.data.apps.AppRepository
import com.tigerpaw.launcher.core.data.apps.PinnedAppsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinnedAppsViewModel @Inject constructor(
    private val pinnedAppsRepository: PinnedAppsRepository,
    appRepository: AppRepository,
) : ViewModel() {

    val pinnedApps: StateFlow<List<AppInfo>> = pinnedAppsRepository.pinnedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allApps: StateFlow<List<AppInfo>> = appRepository.getInstalledApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pin(packageName: String) {
        viewModelScope.launch { pinnedAppsRepository.pin(packageName) }
    }

    fun unpin(packageName: String) {
        viewModelScope.launch { pinnedAppsRepository.unpin(packageName) }
    }

    fun reorder(packageNames: List<String>) {
        viewModelScope.launch { pinnedAppsRepository.reorder(packageNames) }
    }
}
