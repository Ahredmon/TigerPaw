package com.tigerpaw.launcher.core.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
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

        // AI Assistant
        val KEY_AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val KEY_AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val KEY_AI_MODEL = stringPreferencesKey("ai_model")

        // Clock appearance
        val KEY_CLOCK_SIZE = stringPreferencesKey("clock_size")    // "small" | "medium" | "large"
        val KEY_CLOCK_WEIGHT = stringPreferencesKey("clock_weight") // "light" | "regular" | "bold"
        val KEY_CLOCK_ALIGN = stringPreferencesKey("clock_align")  // "start" | "center"

        // Onboarding
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        // Display
        val KEY_FULLSCREEN = booleanPreferencesKey("fullscreen")
        val KEY_SHOW_BATTERY = booleanPreferencesKey("show_battery")

        // Shake-to-assist
        val KEY_SHAKE_ENABLED = booleanPreferencesKey("shake_enabled")
        /** Sensitivity 0..1 mapped to threshold 1800..400 (higher = more sensitive). Default 0.5. */
        val KEY_SHAKE_SENSITIVITY = floatPreferencesKey("shake_sensitivity")

        // Wake word
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
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

    // AI Assistant prefs
    val aiEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AI_ENABLED] ?: false }
    val aiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[KEY_AI_BASE_URL] ?: "http://192.168.0.134:8000"
    }
    val aiModel: Flow<String> = context.dataStore.data.map { it[KEY_AI_MODEL] ?: "" }

    // Clock appearance prefs
    val clockSize: Flow<String> = context.dataStore.data.map { it[KEY_CLOCK_SIZE] ?: "medium" }
    val clockWeight: Flow<String> = context.dataStore.data.map { it[KEY_CLOCK_WEIGHT] ?: "light" }
    val clockAlign: Flow<String> = context.dataStore.data.map { it[KEY_CLOCK_ALIGN] ?: "start" }

    // Onboarding
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    // Display
    val fullscreen: Flow<Boolean> = context.dataStore.data.map { it[KEY_FULLSCREEN] ?: false }
    val showBattery: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_BATTERY] ?: true }

    // Shake-to-assist
    val shakeEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHAKE_ENABLED] ?: true }
    val shakeSensitivity: Flow<Float> = context.dataStore.data.map { it[KEY_SHAKE_SENSITIVITY] ?: 0.5f }

    // Wake word
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WAKE_WORD_ENABLED] ?: false }

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

    suspend fun setAiEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_AI_ENABLED] = value }
    }

    suspend fun setAiBaseUrl(value: String) {
        context.dataStore.edit { it[KEY_AI_BASE_URL] = value }
    }

    suspend fun setAiModel(value: String) {
        context.dataStore.edit { it[KEY_AI_MODEL] = value }
    }

    suspend fun setClockSize(value: String) {
        context.dataStore.edit { it[KEY_CLOCK_SIZE] = value }
    }

    suspend fun setClockWeight(value: String) {
        context.dataStore.edit { it[KEY_CLOCK_WEIGHT] = value }
    }

    suspend fun setClockAlign(value: String) {
        context.dataStore.edit { it[KEY_CLOCK_ALIGN] = value }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun setFullscreen(value: Boolean) {
        context.dataStore.edit { it[KEY_FULLSCREEN] = value }
    }

    suspend fun setShowBattery(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_BATTERY] = value }
    }

    suspend fun setShakeEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_SHAKE_ENABLED] = value }
    }

    suspend fun setShakeSensitivity(value: Float) {
        context.dataStore.edit { it[KEY_SHAKE_SENSITIVITY] = value.coerceIn(0f, 1f) }
    }

    suspend fun setWakeWordEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_WAKE_WORD_ENABLED] = value }
    }
}
