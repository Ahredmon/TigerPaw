package com.tigerpaw.launcher.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLaunchDao {

    @Insert
    suspend fun insert(entity: AppLaunchEntity)

    // ── Global frequency (apps only) ──────────────────────────────────

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopApps(limit: Int): Flow<List<AppUsageSummary>>

    // ── Location context (apps) ───────────────────────────────────────

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsNear(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        limit: Int,
    ): Flow<List<AppUsageSummary>>

    // ── Time-of-day context (apps) ────────────────────────────────────

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
          AND hourOfDay BETWEEN :hourStart AND :hourEnd
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsForHourRange(
        hourStart: Int,
        hourEnd: Int,
        limit: Int,
    ): Flow<List<AppUsageSummary>>

    // ── Day-of-week context (apps) ────────────────────────────────────

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
          AND dayOfWeek = :dayOfWeek
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsForDayOfWeek(dayOfWeek: Int, limit: Int): Flow<List<AppUsageSummary>>

    // ── Combined: location + time-of-day + day-of-week (apps) ─────────

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND hourOfDay BETWEEN :hourStart AND :hourEnd
          AND dayOfWeek = :dayOfWeek
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsForContext(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        hourStart: Int,
        hourEnd: Int,
        dayOfWeek: Int,
        limit: Int,
    ): Flow<List<AppUsageSummary>>

    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND hourOfDay BETWEEN :hourStart AND :hourEnd
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsNearForHourRange(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        hourStart: Int,
        hourEnd: Int,
        limit: Int,
    ): Flow<List<AppUsageSummary>>

    // ── Shortcuts: global frequency ───────────────────────────────────

    @Query(
        """
        SELECT packageName, shortcutId, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'shortcut'
        GROUP BY packageName, shortcutId
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopShortcuts(limit: Int): Flow<List<AppShortcutSummary>>

    // ── Shortcuts: combined context ───────────────────────────────────

    /**
     * Full-context shortcut ranking: same place + same time window + same day of week.
     */
    @Query(
        """
        SELECT packageName, shortcutId, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'shortcut'
          AND latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLon AND :maxLon
          AND hourOfDay BETWEEN :hourStart AND :hourEnd
          AND dayOfWeek = :dayOfWeek
        GROUP BY packageName, shortcutId
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopShortcutsForContext(
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double,
        hourStart: Int,
        hourEnd: Int,
        dayOfWeek: Int,
        limit: Int,
    ): Flow<List<AppShortcutSummary>>

    /**
     * Time-only shortcut ranking (no location required).
     */
    @Query(
        """
        SELECT packageName, shortcutId, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'shortcut'
          AND hourOfDay BETWEEN :hourStart AND :hourEnd
        GROUP BY packageName, shortcutId
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopShortcutsForHourRange(
        hourStart: Int,
        hourEnd: Int,
        limit: Int,
    ): Flow<List<AppShortcutSummary>>

    // ── Metrics: aggregate histograms ─────────────────────────────────

    /** Total launch count per hour of day (0–23), across all launch types. */
    @Query(
        """
        SELECT hourOfDay AS hour, COUNT(*) AS count
        FROM app_launches
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
        """
    )
    fun getLaunchCountByHour(): Flow<List<HourCount>>

    /** Total launch count per ISO day of week (1=Mon … 7=Sun), across all launch types. */
    @Query(
        """
        SELECT dayOfWeek AS day, COUNT(*) AS count
        FROM app_launches
        GROUP BY dayOfWeek
        ORDER BY dayOfWeek ASC
        """
    )
    fun getLaunchCountByDayOfWeek(): Flow<List<DayCount>>

    /** Launches per calendar day for the last [days] days (epoch-ms bucketed by day). */
    @Query(
        """
        SELECT (launchedAt / 86400000) AS dayBucket, COUNT(*) AS count
        FROM app_launches
        WHERE launchedAt >= :sinceMs
        GROUP BY dayBucket
        ORDER BY dayBucket ASC
        """
    )
    fun getLaunchTrend(sinceMs: Long): Flow<List<DayBucketCount>>

    /** Top apps (all time) with launch count, for display in the insights list. */
    @Query(
        """
        SELECT packageName, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'app'
        GROUP BY packageName
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopAppsInsights(limit: Int): Flow<List<AppUsageSummary>>

    /** Top shortcuts (all time) for insights list. */
    @Query(
        """
        SELECT packageName, shortcutId, COUNT(*) AS launchCount
        FROM app_launches
        WHERE launchType = 'shortcut'
        GROUP BY packageName, shortcutId
        ORDER BY launchCount DESC
        LIMIT :limit
        """
    )
    fun getTopShortcutsInsights(limit: Int): Flow<List<AppShortcutSummary>>

    /** Total number of recorded launch events. */
    @Query("SELECT COUNT(*) FROM app_launches")
    fun getTotalLaunchCount(): Flow<Int>
}
