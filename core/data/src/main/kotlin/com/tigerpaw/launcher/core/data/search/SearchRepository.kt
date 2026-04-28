package com.tigerpaw.launcher.core.data.search

import android.content.Context
import android.content.pm.LauncherApps
import android.database.Cursor
import android.net.Uri
import android.os.Process
import android.provider.MediaStore
import com.tigerpaw.launcher.core.data.apps.AppRepository
import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import com.tigerpaw.launcher.core.data.usage.UsageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val APP_LIMIT = 6
private const val FILE_LIMIT = 5
private const val ACTION_LIMIT = 4

@ViewModelScoped
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val prefs: LauncherPreferences,
    private val usageRepository: UsageRepository,
) {
    fun suggestions(): Flow<List<SearchResult.Suggestion>> =
        combine(appRepository.getInstalledApps(), usageRepository.getSuggestions()) { apps, summaries ->
            val appMap = apps.associateBy { it.packageName }
            summaries.mapNotNull { summary ->
                appMap[summary.packageName]?.let { info ->
                    SearchResult.Suggestion(app = info, launchCount = summary.launchCount)
                }
            }
        }

    fun search(query: String): Flow<List<SearchResult>> {
        if (query.isBlank()) return flowOf(emptyList())

        val appsFlow: Flow<List<SearchResult.App>> = combine(
            appRepository.getInstalledApps(),
            prefs.searchIncludeApps,
        ) { apps, include ->
            if (!include) emptyList()
            else apps.filter { it.label.contains(query, ignoreCase = true) }
                .take(APP_LIMIT)
                .map { SearchResult.App(it) }
        }
        val filesFlow: Flow<List<SearchResult.File>> = prefs.searchIncludeFiles.flatMapLatest { include ->
            if (!include) flowOf(emptyList())
            else flow { emit(searchFiles(query)) }.flowOn(Dispatchers.IO)
        }
        val actionsFlow: Flow<List<SearchResult.AppAction>> = prefs.searchIncludeActions.flatMapLatest { include ->
            if (!include) flowOf(emptyList())
            else flow { emit(searchActions(query)) }.flowOn(Dispatchers.IO)
        }

        return combine(appsFlow, filesFlow, actionsFlow) { apps, files, actions ->
            buildList {
                addAll(apps)
                addAll(actions)
                addAll(files)
            }
        }
    }

    // ── Files (MediaStore) ────────────────────────────────────────────

    private fun searchFiles(query: String): List<SearchResult.File> {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
        )
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT $FILE_LIMIT"

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                ?.use { cursor -> cursor.toFileResults(uri) }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun Cursor.toFileResults(baseUri: Uri): List<SearchResult.File> {
        val idCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
        val nameCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
        val mimeCol = getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
        val pathCol = getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
        val results = mutableListOf<SearchResult.File>()
        while (moveToNext()) {
            val id = getLong(idCol)
            val name = getString(nameCol) ?: continue
            val mime = getString(mimeCol) ?: "*/*"
            val path = if (pathCol >= 0) getString(pathCol) ?: "" else ""
            results += SearchResult.File(
                uri = Uri.withAppendedPath(baseUri, id.toString()),
                mimeType = mime,
                label = name,
                subtitle = path.trimEnd('/'),
            )
        }
        return results
    }

    // ── App Actions (Launcher Shortcuts) ─────────────────────────────

    private fun searchActions(query: String): List<SearchResult.AppAction> {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val flags = LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            val shortcutQuery = LauncherApps.ShortcutQuery().apply { setQueryFlags(flags) }
            val user = Process.myUserHandle()
            launcherApps.getShortcuts(shortcutQuery, user)
                ?.filter { shortcut ->
                    val label = shortcut.shortLabel?.toString()
                        ?: shortcut.longLabel?.toString() ?: return@filter false
                    label.contains(query, ignoreCase = true) ||
                            shortcut.`package`.contains(query, ignoreCase = true)
                }
                ?.take(ACTION_LIMIT)
                ?.mapNotNull { shortcut ->
                    val pm = context.packageManager
                    val appLabel = try {
                        pm.getApplicationInfo(shortcut.`package`, 0).loadLabel(pm).toString()
                    } catch (_: Exception) { shortcut.`package` }
                    val icon = try {
                        launcherApps.getShortcutIconDrawable(
                            shortcut,
                            context.resources.displayMetrics.densityDpi,
                        )
                    } catch (_: Exception) { null }
                    val label = shortcut.shortLabel?.toString()
                        ?: shortcut.longLabel?.toString() ?: return@mapNotNull null
                    SearchResult.AppAction(
                        shortcutId = shortcut.id,
                        packageName = shortcut.`package`,
                        shortcutLabel = label,
                        appLabel = appLabel,
                        icon = icon,
                    )
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
