package com.tigerpaw.launcher.feature.widgets

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WidgetsViewModel @Inject constructor(
    private val widgetHostRepository: WidgetHostRepository,
) : ViewModel() {

    val availableWidgets: StateFlow<List<AppWidgetProviderInfo>> =
        widgetHostRepository.getInstalledWidgets()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Sends an explicit bind Intent back to the host Activity so the user can
     * grant BIND_APPWIDGET permission and configure the widget.
     */
    fun requestAddWidget(context: Context, info: AppWidgetProviderInfo) {
        val id = widgetHostRepository.allocateWidgetId()
        val intent = widgetHostRepository.bindWidgetIntent(id, info)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
