package com.tigerpaw.launcher.core.data.usage

import com.tigerpaw.launcher.core.data.db.AppLaunchDao
import com.tigerpaw.launcher.core.data.db.AppLaunchEntity
import com.tigerpaw.launcher.core.data.db.AppShortcutSummary
import com.tigerpaw.launcher.core.data.db.AppUsageSummary
import com.tigerpaw.launcher.core.data.db.LaunchType
import com.tigerpaw.launcher.core.data.location.LocationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

private const val SUGGESTION_LIMIT = 6

/** Degrees of latitude/longitude for the nearby-location bucket (~15 km). */
private const val LOCATION_DELTA = 0.15

/**
 * Half-width of the time-of-day window used for contextual queries (±[TIME_SLOT_HALF_HOURS] hours).
 * A value of 2 means a 4-hour window centred on the current hour.
 */
private const val TIME_SLOT_HALF_HOURS = 2

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UsageRepository @Inject constructor(
    private val dao: AppLaunchDao,
    private val locationProvider: LocationProvider,
) {

    /** Persists an app-launch event with time-of-day, day-of-week, and optional location. */
    suspend fun recordLaunch(packageName: String) {
        val loc = locationProvider.getLastLocation()
        val cal = Calendar.getInstance()
        dao.insert(
            AppLaunchEntity(
                packageName = packageName,
                launchType = LaunchType.APP,
                launchedAt = cal.timeInMillis,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = isoDay(cal.get(Calendar.DAY_OF_WEEK)),
                latitude = loc?.latitude,
                longitude = loc?.longitude,
            )
        )
    }

    /** Persists a shortcut-invocation event with the same context signals as [recordLaunch]. */
    suspend fun recordShortcutLaunch(packageName: String, shortcutId: String) {
        val loc = locationProvider.getLastLocation()
        val cal = Calendar.getInstance()
        dao.insert(
            AppLaunchEntity(
                packageName = packageName,
                launchType = LaunchType.SHORTCUT,
                shortcutId = shortcutId,
                launchedAt = cal.timeInMillis,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = isoDay(cal.get(Calendar.DAY_OF_WEEK)),
                latitude = loc?.latitude,
                longitude = loc?.longitude,
            )
        )
    }

    /**
     * Returns a [Flow] of the most relevant suggested apps using a tiered context strategy:
     *
     * 1. **Full context** — location + time-of-day + day-of-week (strongest signal)
     * 2. **Location + time** — same place, same time window, any day
     * 3. **Location only** — same place, any time
     * 4. **Time only** — same time window, any location
     * 5. **Global** — all-time launch frequency (fallback)
     *
     * Each tier is tried in order; if it yields [SUGGESTION_LIMIT] results the chain stops.
     */
    fun getSuggestions(): Flow<List<AppUsageSummary>> {
        val loc = locationProvider.getLastLocation()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val day = isoDay(cal.get(Calendar.DAY_OF_WEEK))
        val hourStart = maxOf(0, hour - TIME_SLOT_HALF_HOURS)
        val hourEnd = minOf(23, hour + TIME_SLOT_HALF_HOURS)

        return if (loc != null) {
            val minLat = loc.latitude - LOCATION_DELTA
            val maxLat = loc.latitude + LOCATION_DELTA
            val minLon = loc.longitude - LOCATION_DELTA
            val maxLon = loc.longitude + LOCATION_DELTA

            // Tier 1: location + time + day
            dao.getTopAppsForContext(minLat, maxLat, minLon, maxLon, hourStart, hourEnd, day, SUGGESTION_LIMIT)
                .flatMapLatest { tier1 ->
                    if (tier1.size >= SUGGESTION_LIMIT) flowOf(tier1)
                    else
                    // Tier 2: location + time (drop day-of-week constraint)
                    dao.getTopAppsNearForHourRange(minLat, maxLat, minLon, maxLon, hourStart, hourEnd, SUGGESTION_LIMIT)
                        .flatMapLatest { tier2 ->
                            if (tier2.size >= SUGGESTION_LIMIT) flowOf(tier2)
                            else
                            // Tier 3: location only
                            dao.getTopAppsNear(minLat, maxLat, minLon, maxLon, SUGGESTION_LIMIT)
                                .flatMapLatest { tier3 ->
                                    if (tier3.size >= SUGGESTION_LIMIT) flowOf(tier3)
                                    // Tier 4 & 5 handled below
                                    else timeOrGlobalFallback(hourStart, hourEnd, SUGGESTION_LIMIT)
                                }
                        }
                }
        } else {
            timeOrGlobalFallback(hourStart, hourEnd, SUGGESTION_LIMIT)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Tier 4: time-of-day frequency.
     * Tier 5: all-time global frequency (final fallback).
     */
    private fun timeOrGlobalFallback(hourStart: Int, hourEnd: Int, limit: Int): Flow<List<AppUsageSummary>> =
        dao.getTopAppsForHourRange(hourStart, hourEnd, limit)
            .flatMapLatest { timeBased ->
                if (timeBased.size >= limit) flowOf(timeBased)
                else dao.getTopApps(limit)
            }

    /**
     * Returns the most context-relevant shortcuts using a 3-tier fallback:
     * 1. Full context (location + time + day of week)
     * 2. Time-of-day only
     * 3. Global all-time frequency
     */
    fun getShortcutSuggestions(): Flow<List<AppShortcutSummary>> {
        val loc = locationProvider.getLastLocation()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val day = isoDay(cal.get(Calendar.DAY_OF_WEEK))
        val hourStart = maxOf(0, hour - TIME_SLOT_HALF_HOURS)
        val hourEnd = minOf(23, hour + TIME_SLOT_HALF_HOURS)

        return if (loc != null) {
            val minLat = loc.latitude - LOCATION_DELTA
            val maxLat = loc.latitude + LOCATION_DELTA
            val minLon = loc.longitude - LOCATION_DELTA
            val maxLon = loc.longitude + LOCATION_DELTA

            dao.getTopShortcutsForContext(minLat, maxLat, minLon, maxLon, hourStart, hourEnd, day, SUGGESTION_LIMIT)
                .flatMapLatest { tier1 ->
                    if (tier1.size >= SUGGESTION_LIMIT) flowOf(tier1)
                    else shortcutTimeOrGlobalFallback(hourStart, hourEnd, SUGGESTION_LIMIT)
                }
        } else {
            shortcutTimeOrGlobalFallback(hourStart, hourEnd, SUGGESTION_LIMIT)
        }
    }

    private fun shortcutTimeOrGlobalFallback(hourStart: Int, hourEnd: Int, limit: Int): Flow<List<AppShortcutSummary>> =
        dao.getTopShortcutsForHourRange(hourStart, hourEnd, limit)
            .flatMapLatest { timeBased ->
                if (timeBased.size >= limit) flowOf(timeBased)
                else dao.getTopShortcuts(limit)
            }

    /** Converts [Calendar.DAY_OF_WEEK] (Sun=1) to ISO 8601 (Mon=1, Sun=7). */
    private fun isoDay(calendarDay: Int): Int =
        if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1
}
