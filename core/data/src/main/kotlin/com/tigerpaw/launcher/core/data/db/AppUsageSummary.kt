package com.tigerpaw.launcher.core.data.db

data class AppUsageSummary(
    val packageName: String,
    val launchCount: Int,
)

/** Launch count for a single hour of the day (0–23). */
data class HourCount(val hour: Int, val count: Int)

/** Launch count for a single ISO day of week (1=Mon … 7=Sun). */
data class DayCount(val day: Int, val count: Int)

/** Launch count for a day bucket (epoch-ms / 86400000). */
data class DayBucketCount(val dayBucket: Long, val count: Int)
