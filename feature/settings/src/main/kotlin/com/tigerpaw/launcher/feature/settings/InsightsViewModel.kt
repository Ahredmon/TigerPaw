package com.tigerpaw.launcher.feature.settings

import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tigerpaw.launcher.core.data.db.AppShortcutSummary
import com.tigerpaw.launcher.core.data.db.AppUsageSummary
import com.tigerpaw.launcher.core.data.db.DayBucketCount
import com.tigerpaw.launcher.core.data.db.DayCount
import com.tigerpaw.launcher.core.data.db.HourCount
import com.tigerpaw.launcher.core.data.metrics.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class InsightsUiState(
    val totalLaunches: Int = 0,
    val byHour: List<HourCount> = emptyList(),
    val byDay: List<DayCount> = emptyList(),
    val trend: List<DayBucketCount> = emptyList(),
    val topApps: List<AppUsageSummary> = emptyList(),
    val topShortcuts: List<AppShortcutSummary> = emptyList(),
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val metricsRepository: MetricsRepository,
) : ViewModel() {

    val totalLaunches: StateFlow<Int> = metricsRepository.getTotalLaunchCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val byHour: StateFlow<List<HourCount>> = metricsRepository.getLaunchesByHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val byDay: StateFlow<List<DayCount>> = metricsRepository.getLaunchesByDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trend: StateFlow<List<DayBucketCount>> = metricsRepository.getLaunchTrend()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topApps: StateFlow<List<AppUsageSummary>> = metricsRepository.getTopApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topShortcuts: StateFlow<List<AppShortcutSummary>> = metricsRepository.getTopShortcuts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
