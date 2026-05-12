package com.tigerpaw.launcher.core.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.util.Log
import com.tigerpaw.launcher.core.data.search.SearchResult
import com.tigerpaw.launcher.core.data.usage.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
) {
    companion object {
        private const val TAG = "TigerPaw/Apps"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getInstalledApps(): Flow<List<AppInfo>> = flow {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .map { resolveInfo ->
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    icon = resolveInfo.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
        Log.d(TAG, "getInstalledApps: ${apps.size} apps found")
        emit(apps)
    }.flowOn(Dispatchers.IO)

    fun launch(context: Context, app: AppInfo) {
        Log.i(TAG, "launch: ${app.label} (${app.packageName})")
        scope.launch { usageRepository.recordLaunch(app.packageName) }
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(app.packageName, app.activityName)
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "launch failed: ${app.packageName}", e)
        }
    }

    /** Invokes a launcher shortcut and records the event in the usage database. */
    fun launchAction(context: Context, action: SearchResult.AppAction) {
        Log.i(TAG, "launchAction: shortcut=${action.shortcutId} pkg=${action.packageName} label='${action.label}'")
        scope.launch { usageRepository.recordShortcutLaunch(action.packageName, action.shortcutId) }
        try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            launcherApps.startShortcut(action.packageName, action.shortcutId, null, null, Process.myUserHandle())
        } catch (e: Exception) {
            Log.e(TAG, "launchAction failed: ${action.packageName}/${action.shortcutId}", e)
        }
    }
}
