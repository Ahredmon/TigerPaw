package com.tigerpaw.launcher.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A frosted-glass surface for launcher UI elements (dock, search bars, etc.)
 * Uses a semi-transparent background. On API 31+ true blur is applied via Modifier.blur;
 * on older APIs it falls back gracefully.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    tint: Color = Color.White.copy(alpha = 0.15f),
    blurRadius: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .blur(blurRadius)
            .background(tint)
    ) {
        content()
    }
}
