package com.tigerpaw.launcher.feature.settings

import android.net.Uri
import android.Manifest
import android.content.Intent
import android.net.Uri as IntentUri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tigerpaw.launcher.core.ui.components.SpringPressable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onOpenInsights: () -> Unit = {},
    onOpenPinnedApps: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val darkIcons by viewModel.darkIcons.collectAsState()
    val searchIncludeApps by viewModel.searchIncludeApps.collectAsState()
    val searchIncludeFiles by viewModel.searchIncludeFiles.collectAsState()
    val searchIncludeActions by viewModel.searchIncludeActions.collectAsState()
    val aiEnabled by viewModel.aiEnabled.collectAsState()
    val aiBaseUrl by viewModel.aiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val availableModels by viewModel.availableModels.collectAsState()
    val modelsLoading by viewModel.modelsLoading.collectAsState()
    val clockSize by viewModel.clockSize.collectAsState()
    val clockWeight by viewModel.clockWeight.collectAsState()
    val clockAlign by viewModel.clockAlign.collectAsState()
    val fullscreen by viewModel.fullscreen.collectAsState()
    val showBattery by viewModel.showBattery.collectAsState()
    val shakeEnabled by viewModel.shakeEnabled.collectAsState()
    val shakeSensitivity by viewModel.shakeSensitivity.collectAsState()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()

    val wallpaperPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.setWallpaper(it) }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_IMAGES
    else
        Manifest.permission.READ_EXTERNAL_STORAGE
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            storagePermission,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val openAppSettings = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = IntentUri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
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

        // ── Permissions ───────────────────────────────────────────────────────
        val allGranted = permissionsState.permissions.all { it.status.isGranted }
        if (!allGranted) {
            SectionLabel("Permissions")

            Spacer(Modifier.height(8.dp))

            permissionsState.permissions.forEach { perm ->
                PermissionRow(
                    permissionState = perm,
                    label = when (perm.permission) {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_EXTERNAL_STORAGE -> "Storage"
                        Manifest.permission.ACCESS_COARSE_LOCATION -> "Location"
                        else -> perm.permission.substringAfterLast('.')
                    },
                    subtitle = when (perm.permission) {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_EXTERNAL_STORAGE -> "Required for file search results"
                        Manifest.permission.ACCESS_COARSE_LOCATION -> "Used for contextual app suggestions"
                        else -> ""
                    },
                    icon = when (perm.permission) {
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_EXTERNAL_STORAGE -> Icons.Default.Image
                        else -> Icons.Default.LocationOn
                    },
                    onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
                    onOpenSettings = openAppSettings,
                )
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(24.dp))
        }

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

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Fullscreen, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text("Full screen", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text("Hide the status bar", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = fullscreen, onCheckedChange = viewModel::setFullscreen)
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.BatteryFull, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text("Battery indicator", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text("Show battery level under the clock", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = showBattery, onCheckedChange = viewModel::setShowBattery)
        }

        Spacer(Modifier.height(4.dp))

        // Shake-to-assist card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Vibration, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text("Shake to open assistant", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    Text("Shake the device to open the AI screen", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = shakeEnabled, onCheckedChange = viewModel::setShakeEnabled)
            }
            if (shakeEnabled) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Sensitivity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.width(80.dp),
                    )
                    Slider(
                        value = shakeSensitivity,
                        onValueChange = viewModel::setShakeSensitivity,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(start = 80.dp))
                    Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
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

        Spacer(Modifier.height(28.dp))

        SectionLabel("AI Assistant")

        Spacer(Modifier.height(8.dp))

        // Enable / disable toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                null,
                tint = if (aiEnabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    "Enable AI Assistant",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Connect to a local AI server (LocalAI)",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = aiEnabled, onCheckedChange = viewModel::setAiEnabled)
        }

        if (aiEnabled) {
            val focusManager = LocalFocusManager.current
            Spacer(Modifier.height(4.dp))

            Spacer(Modifier.height(4.dp))

            // ── Wake word toggle ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.RecordVoiceOver,
                    null,
                    tint = if (wakeWordEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        "Wake word",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Always listen for \"Hey Tiger\" to open the assistant",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = wakeWordEnabled, onCheckedChange = viewModel::setWakeWordEnabled)
            }

            // ── Server URL ────────────────────────────────────────────────────
            var hostDraft by remember(aiBaseUrl) { mutableStateOf(aiBaseUrl) }
            OutlinedTextField(
                value = hostDraft,
                onValueChange = { hostDraft = it },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.0.134:8000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    viewModel.setAiBaseUrl(hostDraft)
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.setAiBaseUrl(hostDraft) },
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(Modifier.height(4.dp))

            // ── Model selector ────────────────────────────────────────────────
            var modelMenuExpanded by remember { mutableStateOf(false) }
            var modelDraft by remember(aiModel) { mutableStateOf(aiModel) }

            ExposedDropdownMenuBox(
                expanded = modelMenuExpanded && availableModels.isNotEmpty(),
                onExpandedChange = { modelMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = modelDraft,
                    onValueChange = { modelDraft = it },
                    label = { Text("Model") },
                    placeholder = { Text("(server default)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.setAiModel(modelDraft)
                        focusManager.clearFocus()
                    }),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (modelsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(onClick = viewModel::fetchModels) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Fetch models",
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                            if (availableModels.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuExpanded)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                        .onFocusChanged { if (!it.isFocused) viewModel.setAiModel(modelDraft) },
                    shape = RoundedCornerShape(16.dp),
                )

                if (availableModels.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false },
                    ) {
                        availableModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model) },
                                onClick = {
                                    modelDraft = model
                                    viewModel.setAiModel(model)
                                    modelMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            SettingRow(
                icon = Icons.Default.AutoAwesome,
                title = "Open Assistant",
                subtitle = "Chat, ask questions, analyze images",
                onClick = onOpenAiAssistant,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Clock ──────────────────────────────────────────────────────────────
        SectionLabel("Clock")
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {

            // Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    "Size",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    listOf("small", "medium", "large").forEach { opt ->
                        val selected = clockSize == opt
                        SpringPressable(onClick = { viewModel.setClockSize(opt) }) {
                            Text(
                                opt.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            // Style (weight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.InvertColors, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    "Style",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    listOf("light" to "Light", "regular" to "Regular", "bold" to "Bold").forEach { (key, label) ->
                        val selected = clockWeight == key
                        SpringPressable(onClick = { viewModel.setClockWeight(key) }) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }

            // Alignment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Bolt, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text(
                    "Alignment",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                    listOf("start" to "Left", "center" to "Center").forEach { (key, label) ->
                        val selected = clockAlign == key
                        SpringPressable(onClick = { viewModel.setClockAlign(key) }) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
            }
        }

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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRow(
    permissionState: PermissionState,
    label: String,
    subtitle: String,
    icon: ImageVector,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val granted = permissionState.status.isGranted
    val deniedPermanently = !granted && !permissionState.status.shouldShowRationale
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
        }
        if (granted) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Button(
                onClick = if (deniedPermanently) onOpenSettings else onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Default.Warning, null, modifier = Modifier.then(androidx.compose.ui.Modifier.padding(end = 4.dp)))
                Text(if (deniedPermanently) "Open Settings" else "Grant")
            }
        }
    }
}
