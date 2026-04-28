package com.tigerpaw.launcher.feature.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.tigerpaw.launcher.core.data.apps.AppInfo
import com.tigerpaw.launcher.core.ui.components.SpringPressable

@Composable
fun AppsScreen(
    onOpenSettings: () -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val apps by viewModel.apps.collectAsState()
    val query by viewModel.query.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.88f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f))
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = {
                        Text("Search apps", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )

                // Grouped grid — only show section letters when not filtering
                val grouped = remember(apps, query) {
                    if (query.isBlank()) apps.groupBy { it.label.first().uppercaseChar() }
                    else mapOf(' ' to apps)
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    grouped.forEach { (letter, groupApps) ->
                        if (query.isBlank()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = letter.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(
                                        start = 8.dp, top = 12.dp, bottom = 4.dp,
                                    ),
                                )
                            }
                        }
                        items(groupApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                            AppGridItem(app = app, onClick = { viewModel.launch(context, app) })
                        }
                    }
                }
            }
        }
}

@Composable
private fun AppGridItem(app: AppInfo, onClick: () -> Unit) {
    SpringPressable(onClick = onClick) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = app.icon),
                contentDescription = app.label,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
