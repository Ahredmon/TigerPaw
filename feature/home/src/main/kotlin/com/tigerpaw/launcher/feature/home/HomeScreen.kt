package com.tigerpaw.launcher.feature.home

import android.app.WallpaperManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    onOpenWidgets: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    searchViewModel: InlineSearchViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var editModeFlash by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }

    val query by searchViewModel.query.collectAsState()
    val grouped by searchViewModel.results.collectAsState()
    val pinnedApps by viewModel.pinnedApps.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    BackHandler(enabled = searchActive) {
        searchActive = false
        searchViewModel.clearQuery()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(searchActive) {
                if (!searchActive) {
                    detectTapGestures(
                        onLongPress = {
                            scope.launch {
                                editModeFlash = true; delay(160); editModeFlash = false; onOpenWidgets()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Clock()
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
        }

        // Bottom anchor — bar is ALWAYS here, never moves
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
                Text("shortcut", style = MaterialTheme.typography.labelSmall, color = accent.copy(alpha = 0.7f))
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
private fun Clock() {
    val context = LocalContext.current
    val time = remember { LocalTime.now().format(ClockFormatter) }

    val wallpaperLuminance = remember {
        try {
            val colors = WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            colors?.primaryColor?.toArgb()?.let { Color(it).luminance() } ?: 0f
        } catch (_: Exception) { 0f }
    }
    val isLightWallpaper = wallpaperLuminance > 0.35f
    val textColor = if (isLightWallpaper) Color(0xFF1A1A1A) else Color.White
    val shadowColor = if (isLightWallpaper) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.55f)

    Text(
        text = time,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Light,
            shadow = Shadow(color = shadowColor, offset = Offset(0f, 2f), blurRadius = 12f),
        ),
        color = textColor,
    )
}
