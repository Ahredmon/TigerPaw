package com.tigerpaw.launcher.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.ui.components.SpringPressable

@Composable
fun PinnedAppsScreen(
    onBack: () -> Unit = {},
    viewModel: PinnedAppsViewModel = hiltViewModel(),
) {
    val pinned by viewModel.pinnedApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    var query by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    val filtered = remember(query, allApps, pinned) {
        val pinnedPackages = pinned.map { it.packageName }.toSet()
        if (query.isBlank()) allApps.filter { it.packageName !in pinnedPackages }
        else allApps.filter {
            it.packageName !in pinnedPackages &&
                it.label.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Pinned Apps",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            SpringPressable(onClick = { showPicker = !showPicker; query = "" }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (showPicker) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (showPicker) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (showPicker) "Cancel" else "Add app",
                            tint = if (showPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (showPicker) "Cancel" else "Add",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (showPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .animateContentSize(),
        ) {
            // ── Current pinned list ───────────────────────────────────
            if (pinned.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Pinned")
                    Spacer(Modifier.height(8.dp))
                }

                items(pinned, key = { it.packageName }) { app ->
                    PinnedAppRow(
                        app = app,
                        onRemove = { viewModel.unpin(app.packageName) },
                    )
                    Spacer(Modifier.height(4.dp))
                }
            } else if (!showPicker) {
                item {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        "No pinned apps yet. Tap Add to pin apps to your home bar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            // ── App picker ────────────────────────────────────────────
            if (showPicker) {
                item {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Add App")
                    Spacer(Modifier.height(8.dp))
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(filtered, key = { "pick/${it.packageName}" }) { app ->
                    PickableAppRow(
                        app = app,
                        onAdd = {
                            viewModel.pin(app.packageName)
                            // Dismiss picker once something is added
                            showPicker = false
                            query = ""
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                }

                item { Spacer(Modifier.height(24.dp)) }
            } else {
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ── Rows ──────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedAppRow(app: AppInfo, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp),
        )
        Image(
            painter = rememberAsyncImagePainter(model = app.icon),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun PickableAppRow(app: AppInfo, onAdd: () -> Unit) {
    SpringPressable(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = app.icon),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Default.Add,
                contentDescription = "Pin",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, null, tint = onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = onSurface),
            cursorBrush = SolidColor(onSurface),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text("Filter apps…", style = MaterialTheme.typography.bodyMedium, color = onSurface.copy(alpha = 0.35f))
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
        }
    }
}
