package com.tigerpaw.launcher.feature.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val grouped by viewModel.results.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val cs = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Search bar ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = cs.onBackground)
                }
                TextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search apps, files, actions…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(26.dp),
                )
                Spacer(Modifier.width(8.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // ── Results / empty states ──────────────────────────────────
            AnimatedContent(
                targetState = grouped,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "results",
            ) { g ->
                when {
                    query.isBlank() && g.suggestions.isNotEmpty() ->
                        SuggestionsState(g.suggestions, onLaunch = { viewModel.launch(context, it) })
                    query.isBlank() -> SearchIdleState()
                    g.isEmpty -> SearchEmptyState(query)
                    else -> ResultList(
                        grouped = g,
                        onLaunch = { viewModel.launch(context, it) },
                    )
                }
            }
        }
    }
}

// ── Idle state ────────────────────────────────────────────────────────────────

@Composable
private fun SearchIdleState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("Start typing to search", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ── Suggestions state ─────────────────────────────────────────────────────────

@Composable
private fun SuggestionsState(
    suggestions: List<SearchResult.Suggestion>,
    onLaunch: (SearchResult) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { SectionHeader("Suggested for You", MaterialTheme.colorScheme.primary, suggestions.size) }
        items(suggestions, key = { "suggestion/${it.app.packageName}" }) { result ->
            SuggestionResultRow(result = result, onClick = { onLaunch(result) })
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun SearchEmptyState(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No results for \"$query\"", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Grouped result list ───────────────────────────────────────────────────────

@Composable
private fun ResultList(grouped: GroupedResults, onLaunch: (SearchResult) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Apps ──────────────────────────────────────────────────────
        if (grouped.apps.isNotEmpty()) {
            item { SectionHeader("Apps", MaterialTheme.colorScheme.primary, grouped.apps.size) }
            items(grouped.apps, key = { "app/${it.app.packageName}/${it.app.activityName}" }) { result ->
                AppResultRow(result = result, onClick = { onLaunch(result) })
            }
            if (grouped.actions.isNotEmpty() || grouped.files.isNotEmpty()) {
                item { SectionDivider() }
            }
        }

        // ── App Actions ───────────────────────────────────────────────
        if (grouped.actions.isNotEmpty()) {
            item { SectionHeader("Actions", MaterialTheme.colorScheme.tertiary, grouped.actions.size) }
            items(grouped.actions, key = { "action/${it.packageName}/${it.shortcutId}" }) { result ->
                ActionResultRow(result = result, onClick = { onLaunch(result) })
            }
            if (grouped.files.isNotEmpty()) {
                item { SectionDivider() }
            }
        }

        // ── Files ─────────────────────────────────────────────────────
        if (grouped.files.isNotEmpty()) {
            item { SectionHeader("Files", MaterialTheme.colorScheme.secondary, grouped.files.size) }
            items(grouped.files, key = { "file/${it.uri}" }) { result ->
                FileResultRow(result = result, onClick = { onLaunch(result) })
            }
        }
    }
}

// ── Section decorations ───────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, accent: Color, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(3.dp, 16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                ),
                color = accent,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 0.5.dp,
    )
}

// ── App row ───────────────────────────────────────────────────────────────────

@Composable
private fun AppResultRow(result: SearchResult.App, onClick: () -> Unit) {
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = result.app.icon),
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    result.app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Suggestion row ────────────────────────────────────────────────────────────

@Composable
private fun SuggestionResultRow(result: SearchResult.Suggestion, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = result.app.icon),
                contentDescription = null,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Launched ${result.launchCount}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Action row ────────────────────────────────────────────────────────────────

@Composable
private fun ActionResultRow(result: SearchResult.AppAction, onClick: () -> Unit) {
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (result.icon != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = result.icon),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(result.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(result.appLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text("shortcut", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
            }
        }
    }
}

// ── File row ──────────────────────────────────────────────────────────────────

@Composable
private fun FileResultRow(result: SearchResult.File, onClick: () -> Unit) {
    SpringPressable(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val (icon, tint) = fileIconAndTint(result.mimeType)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    result.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                mimeCategory(result.mimeType),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

private fun fileIconAndTint(mime: String): Pair<ImageVector, Color> = when {
    mime.startsWith("image/") -> Icons.Default.Image to Color(0xFF4AE7D0)
    mime.startsWith("video/") -> Icons.Default.VideoFile to Color(0xFF9B59FF)
    mime.startsWith("audio/") -> Icons.Default.AudioFile to Color(0xFFFF9B59)
    mime.startsWith("text/") || mime.contains("document") || mime.contains("pdf") ->
        Icons.Default.Description to Color(0xFFE7C84A)
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

