package com.tigerpaw.launcher.feature.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Gemini-style floating assistant overlay.
 *
 * - Non-fullscreen: a bottom sheet rises over the launcher content.
 * - A semi-transparent scrim fills the rest; tapping it dismisses.
 * - The tiger orb sits centre-stage with spinning / pulsing animations.
 * - Mic starts automatically when the overlay opens.
 */
@Composable
fun AssistantOrbOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onOpenFullChat: () -> Unit,
    viewModel: AssistantOrbViewModel = hiltViewModel(),
) {
    val isMicActive by viewModel.isMicActive.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val response by viewModel.response.collectAsState()

    // Reset + auto-start mic whenever the overlay becomes visible.
    LaunchedEffect(visible) {
        if (visible) {
            viewModel.reset()
            viewModel.startListening()
        }
    }

    // Release mic when overlay closes.
    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(200)),
    ) {
        // Full-screen touch target: tapping the scrim dismisses.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320)) { it } + fadeIn(tween(240)),
        exit = slideOutVertically(tween(280)) { it } + fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}, // block click-through to scrim
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Drag handle ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
                )

                Spacer(Modifier.height(16.dp))

                // ── AI response area ──────────────────────────────────────
                val responseScrollState = rememberScrollState()
                LaunchedEffect(response) { responseScrollState.animateScrollTo(0) }

                if (response.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .verticalScroll(responseScrollState),
                    ) {
                        Text(
                            text = response,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── Live transcript ───────────────────────────────────────
                val hint = when {
                    isThinking            -> "Thinking…"
                    isMicActive           -> "Listening…"
                    transcript.isNotBlank() -> transcript
                    else                  -> "Tap the orb to speak"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isThinking || isMicActive) 1f else 0.5f,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(28.dp))

                // ── Tiger orb ─────────────────────────────────────────────
                TigerOrb(
                    isThinking = isThinking,
                    isMicActive = isMicActive,
                    onClick = viewModel::onOrbTapped,
                    size = 144.dp,
                )

                Spacer(Modifier.height(24.dp))

                // ── Bottom controls row ───────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Open full chat
                    IconButton(onClick = {
                        onDismiss()
                        onOpenFullChat()
                    }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Open full chat",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Mic status indicator
                    Icon(
                        imageVector = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (isMicActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )

                    Spacer(Modifier.weight(1f))

                    // Dismiss
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss assistant",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}
