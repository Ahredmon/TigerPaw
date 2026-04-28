package com.tigerpaw.launcher.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per launch event — covers both direct app launches and shortcut (app-action) invocations.
 *
 * [launchType]  Discriminator: [LaunchType.APP] or [LaunchType.SHORTCUT].
 * [shortcutId]  Non-null only when [launchType] == [LaunchType.SHORTCUT].
 * [hourOfDay]   0–23, derived from [launchedAt] at record time so SQL GROUP BY is cheap.
 * [dayOfWeek]   1 = Monday … 7 = Sunday (ISO-8601), same reasoning.
 * [latitude] / [longitude] are nullable – only present when location permission is granted.
 */
@Entity(
    tableName = "app_launches",
    indices = [
        Index("packageName"),
        Index("launchType"),
        Index("hourOfDay"),
        Index("dayOfWeek"),
    ],
)
data class AppLaunchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    /** Discriminates between a raw app launch and a shortcut invocation. */
    val launchType: String = LaunchType.APP,
    /** Launcher-shortcut ID; null for [LaunchType.APP] events. */
    val shortcutId: String? = null,
    val launchedAt: Long,
    /** Hour of day (0–23) at launch time, local timezone. */
    val hourOfDay: Int,
    /** ISO day-of-week: 1 = Monday, 7 = Sunday. */
    val dayOfWeek: Int,
    val latitude: Double?,
    val longitude: Double?,
)

object LaunchType {
    const val APP = "app"
    const val SHORTCUT = "shortcut"
}
