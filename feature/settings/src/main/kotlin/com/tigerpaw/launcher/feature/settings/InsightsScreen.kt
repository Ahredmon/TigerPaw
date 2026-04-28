package com.tigerpaw.launcher.feature.settings

import android.content.pm.PackageManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tigerpaw.launcher.core.data.db.AppShortcutSummary
import com.tigerpaw.launcher.core.data.db.AppUsageSummary
import com.tigerpaw.launcher.core.data.db.DayBucketCount
import com.tigerpaw.launcher.core.data.db.DayCount
import com.tigerpaw.launcher.core.data.db.HourCount

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun InsightsScreen(
    onBack: () -> Unit = {},
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val totalLaunches by viewModel.totalLaunches.collectAsState()
    val byHour by viewModel.byHour.collectAsState()
    val byDay by viewModel.byDay.collectAsState()
    val trend by viewModel.trend.collectAsState()
    val topApps by viewModel.topApps.collectAsState()
    val topShortcuts by viewModel.topShortcuts.collectAsState()

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = onSurface,
                )
            }
            Text(
                text = "Usage Insights",
                style = MaterialTheme.typography.headlineSmall,
                color = onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Summary stat ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(surface)
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = "$totalLaunches",
                    style = MaterialTheme.typography.displaySmall,
                    color = primary,
                )
                Text(
                    text = "total launches recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurface.copy(alpha = 0.5f),
                )
            }
        }

        if (totalLaunches == 0) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Launch some apps to start seeing patterns here.",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurface.copy(alpha = 0.4f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(40.dp))
            return@Column
        }

        Spacer(Modifier.height(24.dp))

        // ── 14-day trend ──────────────────────────────────────────────
        if (trend.isNotEmpty()) {
            InsightsCard(title = "14-Day Trend") {
                TrendSparkline(
                    data = trend,
                    lineColor = primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Hourly activity ───────────────────────────────────────────
        if (byHour.isNotEmpty()) {
            InsightsCard(title = "Activity by Hour") {
                HourlyBarChart(
                    data = byHour,
                    barColor = primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                )
                Spacer(Modifier.height(6.dp))
                // Axis labels: midnight, 6am, noon, 6pm, midnight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("12a", "6a", "12p", "6p", "12a").forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.35f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Day of week ───────────────────────────────────────────────
        if (byDay.isNotEmpty()) {
            InsightsCard(title = "Activity by Day") {
                DayOfWeekBarChart(
                    data = byDay,
                    barColor = secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DAY_LABELS.forEach { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = onSurface.copy(alpha = 0.35f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Top apps ──────────────────────────────────────────────────
        if (topApps.isNotEmpty()) {
            InsightsCard(title = "Top Apps") {
                val maxCount = topApps.maxOf { it.launchCount }.coerceAtLeast(1)
                topApps.forEach { summary ->
                    AppUsageRow(
                        label = appLabel(summary.packageName),
                        count = summary.launchCount,
                        maxCount = maxCount,
                        barColor = primary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Top shortcuts ─────────────────────────────────────────────
        if (topShortcuts.isNotEmpty()) {
            InsightsCard(title = "Top App Actions") {
                val maxCount = topShortcuts.maxOf { it.launchCount }.coerceAtLeast(1)
                topShortcuts.forEach { summary ->
                    AppUsageRow(
                        label = "${appLabel(summary.packageName)} › ${summary.shortcutId}",
                        count = summary.launchCount,
                        maxCount = maxCount,
                        barColor = secondary,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InsightsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        content()
    }
}

// ── Charts ────────────────────────────────────────────────────────────

@Composable
private fun HourlyBarChart(
    data: List<HourCount>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val countByHour = IntArray(24).also { arr ->
        data.forEach { arr[it.hour.coerceIn(0, 23)] = it.count }
    }
    val maxVal = countByHour.max().coerceAtLeast(1)

    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) { anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Canvas(modifier = modifier) {
        val barW = size.width / 24f
        val gap = barW * 0.2f
        val usableW = barW - gap
        for (hour in 0..23) {
            val fraction = (countByHour[hour].toFloat() / maxVal) * progress
            val barH = size.height * fraction
            val x = hour * barW + gap / 2f
            val y = size.height - barH
            drawRoundRect(
                color = barColor.copy(alpha = 0.3f + 0.7f * fraction),
                topLeft = Offset(x, y),
                size = Size(usableW, barH.coerceAtLeast(2f)),
                cornerRadius = CornerRadius(3f, 3f),
            )
        }
    }
}

@Composable
private fun DayOfWeekBarChart(
    data: List<DayCount>,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    val countByDay = IntArray(7).also { arr ->
        data.forEach { arr[(it.day - 1).coerceIn(0, 6)] = it.count }
    }
    val maxVal = countByDay.max().coerceAtLeast(1)

    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) { anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Canvas(modifier = modifier) {
        val barW = size.width / 7f
        val gap = barW * 0.25f
        val usableW = barW - gap
        for (day in 0..6) {
            val fraction = (countByDay[day].toFloat() / maxVal) * progress
            val barH = size.height * fraction
            val x = day * barW + gap / 2f
            val y = size.height - barH
            drawRoundRect(
                color = barColor.copy(alpha = 0.35f + 0.65f * fraction),
                topLeft = Offset(x, y),
                size = Size(usableW, barH.coerceAtLeast(2f)),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
private fun TrendSparkline(
    data: List<DayBucketCount>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(data) { anim.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val progress = anim.value

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val minBucket = data.minOf { it.dayBucket }
        val maxBucket = data.maxOf { it.dayBucket }.coerceAtLeast(minBucket + 1)
        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)

        fun xFor(bucket: Long) = ((bucket - minBucket).toFloat() / (maxBucket - minBucket)) * size.width
        fun yFor(count: Int) = size.height - (count.toFloat() / maxCount) * size.height * 0.9f

        val points = data.map { Offset(xFor(it.dayBucket), yFor(it.count)) }

        // Fill path
        val fillPath = Path().apply {
            moveTo(points.first().x, size.height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent),
            ),
        )

        // Clip line to progress
        val visibleUpTo = (points.size * progress).toInt().coerceAtLeast(1)
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until visibleUpTo) lineTo(points[i].x, points[i].y)
        }
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )

        // Dot on latest point in visible range
        val lastIdx = (visibleUpTo - 1).coerceIn(0, points.lastIndex)
        drawCircle(lineColor, radius = 4.dp.toPx(), center = points[lastIdx])
    }
}

@Composable
private fun AppUsageRow(
    label: String,
    count: Int,
    maxCount: Int,
    barColor: Color,
) {
    val fraction = count.toFloat() / maxCount
    val anim = remember(label) { Animatable(0f) }
    LaunchedEffect(count) { anim.animateTo(fraction, tween(500, easing = FastOutSlowInEasing)) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(anim.value)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor.copy(alpha = 0.8f)),
            )
        }
    }
}

@Composable
private fun appLabel(packageName: String): String {
    val pm = LocalContext.current.packageManager
    return try {
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName.substringAfterLast('.')
    }
}
