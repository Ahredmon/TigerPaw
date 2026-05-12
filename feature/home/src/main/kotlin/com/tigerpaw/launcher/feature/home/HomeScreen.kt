package com.tigerpaw.launcher.feature.home

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tigerpaw.launcher.core.data.search.SearchResult
import com.tigerpaw.launcher.core.ui.components.SpringPressable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: InlineSearchViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var editModeFlash by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    val clockSize by viewModel.clockSize.collectAsState()
    val clockWeight by viewModel.clockWeight.collectAsState()
    val clockAlign by viewModel.clockAlign.collectAsState()
    val onboardingDone by viewModel.onboardingDone.collectAsState()
    val showBattery by viewModel.showBattery.collectAsState()

    val query by searchViewModel.query.collectAsState()
    val grouped by searchViewModel.results.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = searchActive) {
        searchActive = false
        searchViewModel.clearQuery()
    }

    // Close search when the app loses window focus (e.g. another app comes forward).
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                searchActive = false
                searchViewModel.clearQuery()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(searchActive) {
                if (!searchActive) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                editModeFlash = true; delay(160); editModeFlash = false; onOpenSettings()
                            }
                        },
                    )
                }
            },
    ) {
        AnimatedVisibility(visible = editModeFlash, enter = fadeIn(tween(80)), exit = fadeOut(tween(100))) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))
        }

        // Clock + settings — fade out when search is open
        AnimatedVisibility(
            visible = !searchActive,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                val clockHAlign = when (clockAlign) {
                    "center" -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = clockHAlign,
                ) {
                    Clock(size = clockSize, weight = clockWeight, align = clockAlign)
                    if (showBattery) {
                        Spacer(Modifier.height(10.dp))
                        BatteryIndicator(align = clockAlign)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 20.dp),
        ) {
            // Pinned apps bar: shown above the search bar when not in search mode
            val pinnedBarHeight = 62.dp // pill height 54dp + 8dp gap
            val hasPinned = pinnedApps.isNotEmpty()

            // Results panel: overlay, sits above pinned bar + search bar
            val resultsPanelOffset = if (hasPinned) 62.dp + pinnedBarHeight else 62.dp
            AnimatedVisibility(
                visible = searchActive,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = resultsPanelOffset),
                enter = expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(280)) + fadeIn(tween(200)),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(220)) + fadeOut(tween(160)),
            ) {
                SearchResultsPanel(
                    query = query,
                    grouped = grouped,
                    onLaunch = {
                        searchViewModel.launch(context, it)
                        searchActive = false
                        searchViewModel.clearQuery()
                    },
                )
            }

            // Pinned apps bar — hidden while search is active
            AnimatedVisibility(
                visible = hasPinned && !searchActive,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 62.dp), // sits just above the search pill
                enter = expandVertically(expandFrom = Alignment.Bottom, animationSpec = tween(240)) + fadeIn(tween(180)),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = tween(180)) + fadeOut(tween(140)),
            ) {
                PinnedAppsBar(
                    apps = pinnedApps,
                    onLaunch = { app ->
                        searchViewModel.launch(context, com.tigerpaw.launcher.core.data.search.SearchResult.App(app))
                    },
                )
            }

            // Search bar: always at the bottom
            AnimatedContent(
                targetState = searchActive,
                modifier = Modifier.align(Alignment.BottomCenter),
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "search-bar-morph",
            ) { active ->
                if (active) {
                    ActiveSearchBar(
                        query = query,
                        onQueryChange = searchViewModel::onQueryChange,
                        onClose = { searchActive = false; searchViewModel.clearQuery() },
                        focusRequester = focusRequester,
                    )
                } else {
                    FrostedSearchBar(onClick = { searchActive = true })
                }
            }
        }
    }

    // First-launch onboarding overlay
    if (!onboardingDone) {
        OnboardingOverlay(onDismiss = viewModel::dismissOnboarding)
    }

    LaunchedEffect(searchActive) {
        if (searchActive) focusRequester.requestFocus()
    }
}

// ── Pinned apps bar ───────────────────────────────────────────────────────────

@Composable
private fun PinnedAppsBar(
    apps: List<com.tigerpaw.launcher.core.data.apps.AppInfo>,
    onLaunch: (com.tigerpaw.launcher.core.data.apps.AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(54.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        apps.forEach { app ->
            SpringPressable(onClick = { onLaunch(app) }) {
                Image(
                    painter = rememberAsyncImagePainter(model = app.icon),
                    contentDescription = app.label,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
    }
}

// ── Idle frosted pill ─────────────────────────────────────────────────────────

@Composable
private fun FrostedSearchBar(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val isLightTint = primary.luminance() > 0.4f
    val scrimAlpha = if (isLightTint) 0.30f else 0.15f
    val borderColor = if (isLightTint) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.30f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color.Black.copy(alpha = scrimAlpha))
            .background(primary.copy(alpha = 0.22f))
            .background(Color.White.copy(alpha = 0.10f))
            .border(0.6.dp, borderColor, RoundedCornerShape(27.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Text("Search apps & more…", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.65f))
        }
    }
}

// ── Active text field bar ─────────────────────────────────────────────────────

@Composable
private fun ActiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester,
) {
    val primary = MaterialTheme.colorScheme.primary
    val isLightTint = primary.luminance() > 0.4f
    val scrimAlpha = if (isLightTint) 0.30f else 0.15f
    val borderColor = if (isLightTint) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.30f)
    val contentColor = Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Color.Black.copy(alpha = scrimAlpha))
            .background(primary.copy(alpha = 0.22f))
            .background(Color.White.copy(alpha = 0.10f))
            .border(0.6.dp, borderColor, RoundedCornerShape(27.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close search",
                tint = contentColor.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp),
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = contentColor),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(contentColor),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            "Search apps, files, actions…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = contentColor.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Spacer(Modifier.size(12.dp))
        }
    }
}

// ── Results panel ─────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsPanel(query: String, grouped: GroupedResults, onLaunch: (SearchResult) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val isLightTint = primary.luminance() > 0.4f
    val scrimAlpha = if (isLightTint) 0.55f else 0.40f
    val borderColor = if (isLightTint) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.22f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            // Frosted layering — matches the search bar
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = scrimAlpha * 0.75f),  // top: slightly lighter
                        Color.Black.copy(alpha = scrimAlpha),           // bottom: full scrim
                    )
                )
            )
            .background(primary.copy(alpha = 0.18f))
            .background(Color.White.copy(alpha = 0.07f))
            .border(0.6.dp, borderColor, RoundedCornerShape(20.dp)),
    ) {
        when {
            query.isBlank() && grouped.suggestions.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                items(grouped.suggestions, key = { "suggestion/${it.app.packageName}" }) { r ->
                    AppResultRow(SearchResult.App(r.app), onClick = { onLaunch(r) })
                }
                item { SectionHeader("Suggested for You", MaterialTheme.colorScheme.primary, grouped.suggestions.size) }
            }
            query.isBlank() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Start typing to search", color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
            }
            grouped.isEmpty -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No results for \"$query\"", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
            }
            // reverseLayout = true: first emitted item → visual bottom (closest to bar)
            // Emit order: Apps first (best match → bottom), Files last (→ top)
            // Within each section, header is emitted after items so it floats above them
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(top = 12.dp),
            ) {
                if (grouped.apps.isNotEmpty()) {
                    items(grouped.apps, key = { "app/${it.app.packageName}/${it.app.activityName}" }) { r ->
                        AppResultRow(r, onClick = { onLaunch(r) })
                    }
                    item { SectionHeader("Apps", MaterialTheme.colorScheme.primary, grouped.apps.size) }
                    if (grouped.actions.isNotEmpty() || grouped.files.isNotEmpty()) item { SectionDivider() }
                }
                if (grouped.actions.isNotEmpty()) {
                    items(grouped.actions, key = { "action/${it.packageName}/${it.shortcutId}" }) { r ->
                        ActionResultRow(r, onClick = { onLaunch(r) })
                    }
                    item { SectionHeader("Actions", MaterialTheme.colorScheme.tertiary, grouped.actions.size) }
                    if (grouped.files.isNotEmpty()) item { SectionDivider() }
                }
                if (grouped.files.isNotEmpty()) {
                    items(grouped.files, key = { "file/${it.uri}" }) { r ->
                        FileResultRow(r, onClick = { onLaunch(r) })
                    }
                    item { SectionHeader("Files", MaterialTheme.colorScheme.secondary, grouped.files.size) }
                }
            }
        }
    }
}

// ── Section chrome ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, accent: Color, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.size(8.dp))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp),
                color = accent,
            )
        }
        Box(
            modifier = Modifier.clip(CircleShape).background(accent.copy(alpha = 0.15f)).padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp), color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)
}

// ── Result rows ───────────────────────────────────────────────────────────────

@Composable
private fun AppResultRow(result: SearchResult.App, onClick: () -> Unit) {
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(painter = rememberAsyncImagePainter(model = result.app.icon), contentDescription = null, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)))
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Text(result.app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ActionResultRow(result: SearchResult.AppAction, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (result.icon != null) {
                    Image(painter = rememberAsyncImagePainter(model = result.icon), contentDescription = null, modifier = Modifier.size(26.dp))
                } else {
                    Icon(Icons.Default.Search, null, tint = accent, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                Text(result.appLabel, style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.9f))
            }
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(accent.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(result.appLabel, style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun FileResultRow(result: SearchResult.File, onClick: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.secondary
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val (icon, tint) = fileIconAndTint(result.mimeType)
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(secondary.copy(alpha = 0.1f)).padding(horizontal = 7.dp, vertical = 2.dp)) {
                Text(mimeCategory(result.mimeType), style = MaterialTheme.typography.labelSmall, color = secondary.copy(alpha = 0.7f))
            }
        }
    }
}

private fun fileIconAndTint(mime: String): Pair<ImageVector, Color> = when {
    mime.startsWith("image/") -> Icons.Default.Image to Color(0xFF4AE7D0)
    mime.startsWith("video/") -> Icons.Default.VideoFile to Color(0xFF9B59FF)
    mime.startsWith("audio/") -> Icons.Default.AudioFile to Color(0xFFFF9B59)
    mime.startsWith("text/") || mime.contains("document") || mime.contains("pdf") -> Icons.Default.Description to Color(0xFFE7C84A)
    else -> Icons.Default.InsertDriveFile to Color(0xFFE7C84A)
}

private fun mimeCategory(mime: String): String = when {
    mime.startsWith("image/") -> "image"
    mime.startsWith("video/") -> "video"
    mime.startsWith("audio/") -> "audio"
    mime.contains("pdf") -> "pdf"
    mime.startsWith("text/") -> "text"
    else -> "file"
}

// ── Clock ─────────────────────────────────────────────────────────────────────

@Composable
private fun Clock(size: String, weight: String, align: String) {
    val context = LocalContext.current

    // Tick every second so the display stays current
    var time by remember { mutableStateOf(LocalTime.now().format(ClockFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            time = LocalTime.now().format(ClockFormatter)
            kotlinx.coroutines.delay(1_000L)
        }
    }

    val wallpaperLuminance = remember {
        try {
            val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            colors?.primaryColor?.toArgb()?.let { Color(it).luminance() } ?: 0f
        } catch (_: Exception) { 0f }
    }
    val isLightWallpaper = wallpaperLuminance > 0.35f
    val textColor = if (isLightWallpaper) Color(0xFF1A1A1A) else Color.White
    val shadowColor = if (isLightWallpaper) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.55f)

    val fontSize = when (size) {
        "small" -> 40.sp
        "large" -> 80.sp
        else    -> 56.sp
    }
    val fontWeight = when (weight) {
        "regular" -> FontWeight.Normal
        "bold"    -> FontWeight.Bold
        else      -> FontWeight.Light
    }
    val textAlign = if (align == "center") TextAlign.Center else TextAlign.Start

    Text(
        text = time,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = fontWeight,
            fontSize = fontSize,
            shadow = Shadow(color = shadowColor, offset = Offset(0f, 2f), blurRadius = 12f),
        ),
        color = textColor,
        textAlign = textAlign,
    )
}

// ── Onboarding overlay ────────────────────────────────────────────────────────

@Composable
private fun OnboardingOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Welcome to TigerPaw",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Swipe up or tap the search bar to find apps and files.\n\nLong-press anywhere on the home screen to open Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Got it")
            }
        }
    }
}

// ── Battery indicator ─────────────────────────────────────────────────────────

private data class BatteryState(val level: Int, val charging: Boolean)

private fun getBatteryState(context: Context): BatteryState {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val pct = if (scale > 0) (level * 100 / scale).coerceIn(0, 100) else 0
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                   status == BatteryManager.BATTERY_STATUS_FULL
    return BatteryState(pct, charging)
}

@Composable
private fun BatteryIndicator(align: String) {
    val context = LocalContext.current

    var battery by remember { mutableStateOf(getBatteryState(context)) }

    // Poll every 5 seconds so charging state is always current, even when the
    // sticky ACTION_BATTERY_CHANGED broadcast isn't re-delivered.
    LaunchedEffect(Unit) {
        while (true) {
            battery = getBatteryState(context)
            kotlinx.coroutines.delay(5_000L)
        }
    }

    // Also update immediately on any battery broadcast (level ticks, plug/unplug).
    androidx.compose.runtime.DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                battery = getBatteryState(ctx)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    val fraction = battery.level / 100f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "battery-fill",
    )

    // Charging pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "charging-pulse")
    val chargePulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "charge-alpha",
    )

    // Colors keyed on level
    val baseColor = when {
        battery.charging -> Color(0xFF4ADE80)  // green while charging
        fraction <= 0.15f -> Color(0xFFEF4444) // red critical
        fraction <= 0.30f -> Color(0xFFFACC15) // amber low
        else -> Color.White.copy(alpha = 0.85f)
    }
    val fillColor = if (battery.charging) baseColor.copy(alpha = chargePulse) else baseColor
    val trackColor = Color.White.copy(alpha = 0.15f)
    val barWidth = 200.dp
    val barHeight = 10.dp

    val horizontalAlignment = if (align == "center") Alignment.CenterHorizontally else Alignment.Start

    Column(
        modifier = Modifier.wrapContentHeight(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Bar + end cap
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(trackColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(barHeight)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (battery.charging)
                                    listOf(fillColor.copy(alpha = 0.7f), fillColor)
                                else
                                    listOf(fillColor.copy(alpha = 0.6f), fillColor),
                            ),
                        )
                        .clip(RoundedCornerShape(50)),
                )
                // Sheen overlay
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                            ),
                        ),
                )
            }
        }

        // Percentage + charging label
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${battery.level}%",
                style = MaterialTheme.typography.labelSmall.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 1f), blurRadius = 4f),
                ),
                color = fillColor,
            )
            if (battery.charging) {
                Text(
                    text = "⚡",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF4ADE80).copy(alpha = chargePulse),
                )
            }
        }
    }
}
