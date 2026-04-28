package com.tigerpaw.launcher.core.data.apps

import com.tigerpaw.launcher.core.data.prefs.LauncherPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinnedAppsRepository @Inject constructor(
    private val appRepository: AppRepository,
    private val prefs: LauncherPreferences,
) {
    /**
     * Ordered list of [AppInfo] for the user's pinned apps.
     * Preserves the user's pin order; silently drops any package no longer installed.
     */
    val pinnedApps: Flow<List<AppInfo>> = combine(
        prefs.pinnedAppPackages,
        appRepository.getInstalledApps(),
    ) { packages, installed ->
        val byPackage = installed.associateBy { it.packageName }
        packages.mapNotNull { byPackage[it] }
    }

    suspend fun pin(packageName: String) {
        val current = prefs.pinnedAppPackages.first()
        if (packageName !in current) prefs.setPinnedApps(current + packageName)
    }

    suspend fun unpin(packageName: String) {
        val current = prefs.pinnedAppPackages.first()
        prefs.setPinnedApps(current - packageName)
    }

    suspend fun reorder(packageNames: List<String>) {
        prefs.setPinnedApps(packageNames)
    }
}
