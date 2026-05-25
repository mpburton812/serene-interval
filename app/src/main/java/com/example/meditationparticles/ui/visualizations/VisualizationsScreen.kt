package com.example.meditationparticles.ui.visualizations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.canvas.CalmingVisualizationCanvas
import com.example.meditationparticles.domain.visualizations.CalmingVisualization
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.SereneTabBackground
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.audio.AmbientAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VisualizationsScreen(
    onOpenVisualization: (CalmingVisualizationId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisualizationsViewModel = viewModel(),
) {
    val visualizations by viewModel.visualizations.collectAsState()

    SereneTabBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Visual Sanctuary",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Choose a landscape to ground your focus.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
            visualizations.take(2).forEach { viz ->
                VisualizationGalleryCard(
                    visualization = viz,
                    onClick = { onOpenVisualization(viz.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
            ) {
                visualizations.drop(2).forEach { viz ->
                    VisualizationGalleryCard(
                        visualization = viz,
                        onClick = { onOpenVisualization(viz.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SereneSpacing.stackMd))
        }
    }
}

@Composable
private fun VisualizationGalleryCard(
    visualization: CalmingVisualization,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .height(visualization.galleryHeightDp.dp)
            .clickable(onClick = onClick),
        cornerRadius = 24.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CalmingVisualizationCanvas(
                visualizationId = visualization.id,
                backgroundTop = visualization.backgroundTop,
                backgroundBottom = visualization.backgroundBottom,
                isPlaying = true,
                particleCount = 60,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = visualization.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Text(
                    text = visualization.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
fun VisualizationPlayerScreen(
    visualization: CalmingVisualization,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val sessionRepository = remember { AppGraph.sessions(context) }
    val audioPlayer = remember { AmbientAudioPlayer(context) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isPlaying, isMuted, visualization.sound) {
        audioPlayer.sync(
            sound = visualization.sound,
            shouldPlay = isPlaying && !isMuted,
        )
    }

    LaunchedEffect(isPlaying, controlsVisible) {
        if (isPlaying && controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    DisposableEffect(visualization.id) {
        val sessionStartMs = System.currentTimeMillis()
        onDispose {
            audioPlayer.release()
            val durationSeconds = ((System.currentTimeMillis() - sessionStartMs) / 1000L).toInt()
            CoroutineScope(Dispatchers.IO).launch {
                sessionRepository.logVisualization(visualization.title, durationSeconds)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                controlsVisible = !controlsVisible
            },
    ) {
        CalmingVisualizationCanvas(
            visualizationId = visualization.id,
            backgroundTop = visualization.backgroundTop,
            backgroundBottom = visualization.backgroundBottom,
            isPlaying = isPlaying,
            particleCount = 320,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f),
                            ),
                        ),
                    )
                    .padding(SereneSpacing.containerMargin),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = visualization.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                        )
                    }
                }

                IconButton(
                    onClick = {
                        isPlaying = !isPlaying
                        if (isPlaying) controlsVisible = true
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(88.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }

                Text(
                    text = "Breathing in rhythm with the visual",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
