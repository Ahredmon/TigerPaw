package com.tigerpaw.launcher.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tigerpaw.launcher.core.ui.components.SpringPressable

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenPinnedApps: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val darkIcons by viewModel.darkIcons.collectAsState()
    val searchIncludeApps by viewModel.searchIncludeApps.collectAsState()
    val searchIncludeFiles by viewModel.searchIncludeFiles.collectAsState()
    val searchIncludeActions by viewModel.searchIncludeActions.collectAsState()

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.setWallpaper(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("Insights")

        Spacer(Modifier.height(8.dp))

        SettingRow(
            icon = Icons.Default.BarChart,
            title = "Usage Insights",
            subtitle = "Charts and patterns from your launch history",
            onClick = onOpenInsights,
        )

        Spacer(Modifier.height(28.dp))

        SectionLabel("Home Screen")

        Spacer(Modifier.height(8.dp))

        SettingRow(
            icon = Icons.Default.PushPin,
            title = "Pinned Apps",
            subtitle = "Manage apps shown in the home screen app bar",
            onClick = onOpenPinnedApps,
        )

        Spacer(Modifier.height(28.dp))

        SectionLabel("Appearance")

        Spacer(Modifier.height(8.dp))

        SettingRow(
            icon = Icons.Default.Image,
            title = "Wallpaper",
            subtitle = "Choose from photos",
            onClick = { wallpaperPicker.launch("image/*") },
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.InvertColors, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text("Dark icon text", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text("Use dark labels over light wallpapers", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = darkIcons, onCheckedChange = viewModel::setDarkIcons)
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("Search Sources")

        Spacer(Modifier.height(8.dp))

        SearchSourceToggle(
            icon = Icons.Default.Apps,
            title = "Apps",
            subtitle = "Include installed apps in search results",
            checked = searchIncludeApps,
            onCheckedChange = viewModel::setSearchIncludeApps,
        )

        Spacer(Modifier.height(4.dp))

        SearchSourceToggle(
            icon = Icons.Default.Bolt,
            title = "App Actions",
            subtitle = "Include app shortcuts and quick actions",
            checked = searchIncludeActions,
            onCheckedChange = viewModel::setSearchIncludeActions,
        )

        Spacer(Modifier.height(4.dp))

        SearchSourceToggle(
            icon = Icons.Default.Folder,
            title = "Files",
            subtitle = "Include device files from storage",
            checked = searchIncludeFiles,
            onCheckedChange = viewModel::setSearchIncludeFiles,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SpringPressable(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SearchSourceToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (checked) 1f else 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                title,
                color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (checked) 0.5f else 0.3f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
