package com.tigerpaw.launcher.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme()

@Composable
fun TigerPawTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Launcher always renders over the live wallpaper — always use dark dynamic colors
    // so Material You tokens (primary, surface, etc.) derive from the current wallpaper.
    val colorScheme = if (dynamicColor) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
