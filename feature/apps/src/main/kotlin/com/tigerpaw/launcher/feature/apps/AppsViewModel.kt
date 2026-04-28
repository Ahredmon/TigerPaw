package com.tigerpaw.launcher.feature.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.data.apps.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
) : ViewModel() {

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = combine(_allApps, _query) { all, q ->
        if (q.isBlank()) all
        else all.filter { it.label.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            appRepository.getInstalledApps().collect { _allApps.value = it }
        }
    }

    fun onQueryChange(q: String) { _query.value = q }

    fun launch(context: Context, app: AppInfo) {
        appRepository.launch(context, app)
    }
}
