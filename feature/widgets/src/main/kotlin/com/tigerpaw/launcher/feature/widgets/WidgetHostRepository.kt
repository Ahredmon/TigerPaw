package com.tigerpaw.launcher.feature.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

const val APPWIDGET_HOST_ID = 0x54504157 // "TPAW" in hex

@Singleton
class WidgetHostRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val host: AppWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)
    private val manager = AppWidgetManager.getInstance(context)

    fun getInstalledWidgets(): Flow<List<AppWidgetProviderInfo>> = flow {
        emit(manager.installedProviders)
    }.flowOn(Dispatchers.IO)

    fun allocateWidgetId(): Int = host.allocateAppWidgetId()

    fun releaseWidgetId(id: Int) = host.deleteAppWidgetId(id)

    /** Returns an Intent to configure the widget via the system picker. */
    fun bindWidgetIntent(appWidgetId: Int, info: AppWidgetProviderInfo): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, Bundle())
        }
    }

    fun startListening() = host.startListening()
    fun stopListening() = host.stopListening()
}
