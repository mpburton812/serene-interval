package com.example.meditationparticles.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.BreathingVisualMode
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import kotlinx.coroutines.isActive

enum class BreathingCanvasDisplayMode {
    Preview,
    Exercise,
}

private const val PREVIEW_ZONE_SHRINK = 0.14f

@Composable
fun BreathingAtmosphereBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawAtmosphericBackground()
        val topInsetPx = size.height * 0.08f
        val bottomInsetPx = size.height * 0.18f
        drawCenterGlow(
            size.width / 2f,
            topInsetPx + (size.height - topInsetPx - bottomInsetPx) * 0.45f,
        )
    }
}

@Composable
fun BreathingCanvas(
    sessionState: BreathingSessionState,
    displayMode: BreathingCanvasDisplayMode,
    visualMode: BreathingVisualMode = BreathingVisualMode.A,
    modifier: Modifier = Modifier,
    topInset: Dp = Dp(0f),
    bottomInset: Dp = Dp(72f),
) {
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }
    val smokeSystem = remember { BreathSmokeSystem() }
    val glassOverlay = painterResource(R.drawable.breath_glass_sphere)
    val pipeTexture = painterResource(R.drawable.breath_pipe_texture)

    val animateSession = displayMode == BreathingCanvasDisplayMode.Exercise && sessionState.isRunning
    LaunchedEffect(animateSession) {
        if (!animateSession) return@LaunchedEffect
        while (isActive) {
            withFrameNanos { nanos ->
                timeMs = nanos / 1_000_000L
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            },
    ) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@Canvas

        val topInsetPx = topInset.toPx()
        val bottomInsetPx = bottomInset.toPx()
        val zoneHeight = canvasHeight - topInsetPx - bottomInsetPx
        val previewExtra = if (displayMode == BreathingCanvasDisplayMode.Preview) {
            zoneHeight * PREVIEW_ZONE_SHRINK
        } else {
            0f
        }

        val layout = when (visualMode) {
            BreathingVisualMode.A -> computeStructureLayout(
                pattern = sessionState.pattern,
                width = canvasWidth,
                height = canvasHeight,
                topInset = topInsetPx + previewExtra,
                bottomInset = bottomInsetPx + previewExtra,
                zoneFillRatio = if (displayMode == BreathingCanvasDisplayMode.Preview) {
                    PREVIEW_ZONE_FILL_RATIO
                } else {
                    FLOW_CHAIN_FILL_RATIO
                },
            )
            BreathingVisualMode.B -> computeModeBLayout(
                pattern = sessionState.pattern,
                width = canvasWidth,
                height = canvasHeight,
                topInset = topInsetPx + previewExtra,
                bottomInset = bottomInsetPx + previewExtra,
                zoneFillRatio = if (displayMode == BreathingCanvasDisplayMode.Preview) {
                    PREVIEW_ZONE_FILL_RATIO
                } else {
                    FLOW_CHAIN_FILL_RATIO
                },
            )
        }

        val visuals = when (displayMode) {
            BreathingCanvasDisplayMode.Preview -> computePreviewSphereVisuals(layout)
            BreathingCanvasDisplayMode.Exercise -> when (visualMode) {
                BreathingVisualMode.A -> computeSphereVisuals(sessionState, layout)
                BreathingVisualMode.B -> computeModeBSphereVisuals(sessionState, layout)
            }
        }

        val startSphereId = if (displayMode == BreathingCanvasDisplayMode.Preview) {
            breathingStartSphereId(layout)
        } else {
            null
        }

        drawBreathingStructure(
            layout = layout,
            visuals = visuals,
            startSphereId = null,
            glassOverlay = glassOverlay,
            pipeTexture = pipeTexture,
        )

        if (displayMode == BreathingCanvasDisplayMode.Exercise) {
            val smokeTargets = collectSmokeTargets(visuals, layout)
            smokeSystem.update(smokeTargets, timeMs, layout.scale)

            val sphereById = layout.allSpheres.associateBy { it.id }
            smokeSystem.activeParticles().forEach { particle ->
                val sphere = sphereById[particle.sphereId] ?: return@forEach
                val clip = Path().apply {
                    addOval(
                        Rect(
                            sphere.center.x - sphere.radius * 0.92f,
                            sphere.center.y - sphere.radius * 0.92f,
                            sphere.center.x + sphere.radius * 0.92f,
                            sphere.center.y + sphere.radius * 0.92f,
                        ),
                    )
                }
                clipPath(clip) {
                    drawSmokeParticle(
                        particle = particle,
                        roleColor = roleColor(sphere.role),
                        scale = layout.scale,
                    )
                }
            }
        }

        drawBreathingStructureGlass(
            layout = layout,
            visuals = visuals,
            startSphereId = startSphereId,
            glassOverlay = glassOverlay,
            scale = layout.scale,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAtmosphericBackground() {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SerenePrimaryContainer.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.35f),
            radius = size.width * 0.55f,
        ),
        radius = size.width * 0.55f,
        center = Offset(size.width * 0.5f, size.height * 0.35f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SereneSecondaryContainer.copy(alpha = 0.1f), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.65f),
            radius = size.width * 0.5f,
        ),
        radius = size.width * 0.5f,
        center = Offset(size.width * 0.5f, size.height * 0.65f),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenterGlow(cx: Float, cy: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF8ECAE6).copy(alpha = 0.22f),
                Color(0xFF8ECAE6).copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.35f,
        ),
        radius = size.width * 0.35f,
        center = Offset(cx, cy),
    )
}
