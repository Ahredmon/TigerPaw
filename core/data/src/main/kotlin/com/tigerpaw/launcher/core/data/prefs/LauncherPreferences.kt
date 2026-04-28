package com.tigerpaw.launcher.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

@Singleton
class LauncherPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val KEY_DARK_ICONS = booleanPreferencesKey("dark_icons")
        val KEY_SEARCH_INCLUDE_APPS = booleanPreferencesKey("search_include_apps")
        val KEY_SEARCH_INCLUDE_FILES = booleanPreferencesKey("search_include_files")
        val KEY_SEARCH_INCLUDE_ACTIONS = booleanPreferencesKey("search_include_actions")
        /** Comma-separated list of package names in user-defined display order. */
        val KEY_PINNED_APPS = stringPreferencesKey("pinned_apps")
    }

    val wallpaperUri: Flow<String?> = context.dataStore.data.map { it[KEY_WALLPAPER_URI] }
    val darkIcons: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_ICONS] ?: false }

    val searchIncludeApps: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SEARCH_INCLUDE_APPS] ?: true }
    val searchIncludeFiles: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SEARCH_INCLUDE_FILES] ?: true }
    val searchIncludeActions: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SEARCH_INCLUDE_ACTIONS] ?: true }

    /** Ordered list of pinned package names. Empty list = no pinned apps. */
    val pinnedAppPackages: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_PINNED_APPS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setWallpaperUri(uri: String) {
        context.dataStore.edit { it[KEY_WALLPAPER_URI] = uri }
    }

    suspend fun setDarkIcons(value: Boolean) {
        context.dataStore.edit { it[KEY_DARK_ICONS] = value }
    }

    suspend fun setSearchIncludeApps(value: Boolean) {
        context.dataStore.edit { it[KEY_SEARCH_INCLUDE_APPS] = value }
    }

    suspend fun setSearchIncludeFiles(value: Boolean) {
        context.dataStore.edit { it[KEY_SEARCH_INCLUDE_FILES] = value }
    }

    suspend fun setSearchIncludeActions(value: Boolean) {
        context.dataStore.edit { it[KEY_SEARCH_INCLUDE_ACTIONS] = value }
    }

    suspend fun setPinnedApps(packageNames: List<String>) {
        context.dataStore.edit { it[KEY_PINNED_APPS] = packageNames.joinToString(",") }
    }
}
