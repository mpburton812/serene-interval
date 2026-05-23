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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.ui.theme.BreathSandExhale
import com.example.meditationparticles.ui.theme.BreathSandHold
import com.example.meditationparticles.ui.theme.BreathSandInhale
import com.example.meditationparticles.ui.theme.GlassBorder
import com.example.meditationparticles.ui.theme.GlassFill
import com.example.meditationparticles.ui.theme.PipeMetal
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import kotlinx.coroutines.isActive

@Composable
fun BreathingCanvas(
    sessionState: BreathingSessionState,
    modifier: Modifier = Modifier,
    topInset: Dp = 120.dp,
    bottomInset: Dp = 280.dp,
) {
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }
    val moteSystem = remember { SandMoteSystem() }

    LaunchedEffect(Unit) {
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

        drawAtmosphericBackground()
        drawCenterGlow(canvasWidth / 2f, topInsetPx + (canvasHeight - topInsetPx - bottomInsetPx) * 0.45f)

        val layout = computeStructureLayout(canvasWidth, canvasHeight, topInsetPx, bottomInsetPx)
        val visuals = computeSphereVisuals(sessionState, layout)

        drawPipes(layout)
        layout.allSpheres.forEach { sphere ->
            val visual = visuals[sphere.id] ?: SphereVisualState(0f, false)
            drawBreathSphere(sphere, visual.fillLevel, layout.scale)
        }

        val activeSphere = activeMoteSphere(sessionState, layout, visuals)
        val activeFill = activeSphere?.let { visuals[it.id]?.fillLevel } ?: 0f
        moteSystem.update(activeSphere, activeFill, timeMs, layout.scale)

        if (activeSphere != null && activeFill > 0f) {
            val moteColor = roleColor(activeSphere.role)
            val visible = moteSystem.visibleMoteCount(activeFill)
            moteSystem.motes.take(visible).forEach { mote ->
                val r = mote.size * layout.scale
                val center = Offset(mote.x, mote.y)
                drawCircle(
                    color = moteColor.copy(alpha = 0.35f),
                    radius = r * 2f,
                    center = center,
                )
                drawCircle(
                    color = moteColor.copy(alpha = 0.9f),
                    radius = r,
                    center = center,
                )
            }
        }
    }
}

private fun roleColor(role: SphereRole): Color = when (role) {
    SphereRole.InhaleBlue -> BreathSandInhale
    SphereRole.ExhaleRed -> BreathSandExhale
    SphereRole.HoldPurple -> BreathSandHold
}

private fun DrawScope.drawAtmosphericBackground() {
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

private fun DrawScope.drawCenterGlow(cx: Float, cy: Float) {
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

private fun DrawScope.drawPipes(layout: BreathStructureLayout) {
    val w = 11f * layout.scale.coerceIn(0.85f, 1.25f)
    val top = layout.topHold
    val bottom = layout.bottomHold
    val p0 = layout.pairs[0]
    val p1 = layout.pairs[1]
    val p2 = layout.pairs[2]
    val p3 = layout.pairs[3]

    fun pipe(a: Offset, b: Offset) {
        drawLine(PipeMetal, a, b, w, StrokeCap.Round)
        drawLine(PipeMetal.copy(alpha = 0.35f), a, b, w * 1.35f, StrokeCap.Round)
    }

    // Top hold → first pair
    pipe(sphereEdge(top.center, top.radius, p0.first.center), p0.first.center)
    pipe(sphereEdge(top.center, top.radius, p0.second.center), p0.second.center)

    // X-ladder between pairs
    pipe(p0.first.center, p1.second.center)
    pipe(p0.second.center, p1.first.center)
    pipe(p1.first.center, p2.second.center)
    pipe(p1.second.center, p2.first.center)
    pipe(p2.first.center, p3.second.center)
    pipe(p2.second.center, p3.first.center)

    // Last pair → bottom hold
    pipe(p3.first.center, sphereEdge(bottom.center, bottom.radius, p3.first.center))
    pipe(p3.second.center, sphereEdge(bottom.center, bottom.radius, p3.second.center))
}

private fun sphereEdge(from: Offset, radius: Float, toward: Offset): Offset {
    val dx = toward.x - from.x
    val dy = toward.y - from.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    return Offset(from.x + dx / len * radius * 0.92f, from.y + dy / len * radius * 0.92f)
}

private fun DrawScope.drawBreathSphere(
    sphere: BreathSphere,
    fillLevel: Float,
    scale: Float,
) {
    val roleColor = roleColor(sphere.role)
    val fill = fillLevel.coerceIn(0f, 1f)
    val center = sphere.center
    val radius = sphere.radius

    // Soft glow when filling
    if (fill in 0.05f..0.99f) {
        drawCircle(
            color = roleColor.copy(alpha = 0.18f * fill),
            radius = radius * 1.45f,
            center = center,
        )
    }

    // Translucent glass base
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                GlassFill.copy(alpha = 0.45f),
                GlassFill.copy(alpha = 0.15f),
            ),
            center = center + Offset(-radius * 0.3f, -radius * 0.35f),
            radius = radius * 1.3f,
        ),
        radius = radius,
        center = center,
    )

    // Color fill — sphere transitions from glass to solid role color
    if (fill > 0f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    roleColor.copy(alpha = 0.95f * fill),
                    roleColor.copy(alpha = 0.75f * fill),
                    roleColor.copy(alpha = 0.55f * fill),
                ),
                center = center + Offset(-radius * 0.15f, -radius * 0.2f),
                radius = radius * 1.05f,
            ),
            radius = radius * 0.96f,
            center = center,
        )
    }

    drawCircle(
        color = GlassBorder.copy(alpha = 0.65f + 0.25f * fill),
        radius = radius,
        center = center,
        style = Stroke(width = (1.8f * scale).coerceAtLeast(1f)),
    )

    drawArc(
        color = Color.White.copy(alpha = 0.4f + 0.2f * fill),
        startAngle = 205f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.88f),
        size = androidx.compose.ui.geometry.Size(radius * 1.55f, radius * 1.15f),
        style = Stroke(width = (2.2f * scale).coerceAtLeast(1f), cap = StrokeCap.Round),
    )
}
