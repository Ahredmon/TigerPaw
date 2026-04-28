package com.tigerpaw.launcher.core.data.metrics

import com.tigerpaw.launcher.core.data.db.AppLaunchDao
import com.tigerpaw.launcher.core.data.db.AppShortcutSummary
import com.tigerpaw.launcher.core.data.db.AppUsageSummary
import com.tigerpaw.launcher.core.data.db.DayBucketCount
import com.tigerpaw.launcher.core.data.db.DayCount
import com.tigerpaw.launcher.core.data.db.HourCount
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TOP_N = 10
private const val TREND_DAYS = 14L

@Singleton
class MetricsRepository @Inject constructor(private val dao: AppLaunchDao) {

    fun getTotalLaunchCount(): Flow<Int> = dao.getTotalLaunchCount()

    /** Launches per hour of day (0–23). Missing hours not included — caller pads to 24. */
    fun getLaunchesByHour(): Flow<List<HourCount>> = dao.getLaunchCountByHour()

    /** Launches per ISO day of week (1=Mon … 7=Sun). */
    fun getLaunchesByDay(): Flow<List<DayCount>> = dao.getLaunchCountByDayOfWeek()

    /** Per-day launch counts for the last [TREND_DAYS] days. */
    fun getLaunchTrend(): Flow<List<DayBucketCount>> {
        val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(TREND_DAYS)
        return dao.getLaunchTrend(since)
    }

    fun getTopApps(): Flow<List<AppUsageSummary>> = dao.getTopAppsInsights(TOP_N)

    fun getTopShortcuts(): Flow<List<AppShortcutSummary>> = dao.getTopShortcutsInsights(TOP_N)
}
