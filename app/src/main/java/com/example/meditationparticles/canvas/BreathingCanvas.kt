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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.FillDirection
import com.example.meditationparticles.ui.theme.BreathSandExhale
import com.example.meditationparticles.ui.theme.BreathSandHold
import com.example.meditationparticles.ui.theme.BreathSandInhale
import com.example.meditationparticles.ui.theme.GlassBorder
import com.example.meditationparticles.ui.theme.PipeMetal
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import kotlinx.coroutines.isActive
import kotlin.math.atan2

@Composable
fun BreathingCanvas(
    sessionState: BreathingSessionState,
    modifier: Modifier = Modifier,
    topInset: Dp = Dp(0f),
    bottomInset: Dp = Dp(72f),
) {
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var timeMs by remember { mutableLongStateOf(0L) }
    val smokeSystem = remember { DirectionalSmokeSystem() }
    val glassPainter = painterResource(R.drawable.breath_glass_sphere_empty)
    val pipePainter = painterResource(R.drawable.breath_pipe_straight)

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

        val layout = computeStructureLayout(
            pattern = sessionState.pattern,
            width = canvasWidth,
            height = canvasHeight,
            topInset = topInsetPx,
            bottomInset = bottomInsetPx,
        )
        val visuals = computeSphereVisuals(sessionState, layout)

        layout.pipes.forEach { (a, b) ->
            drawPipeSegment(a, b, layout.scale, pipePainter)
        }

        layout.allSpheres.forEach { sphere ->
            val visual = visuals[sphere.id] ?: SphereVisualState(0f, false)
            drawBreathSphere(
                sphere = sphere,
                visual = visual,
                scale = layout.scale,
                glassPainter = glassPainter,
            )
        }

        val activeSphere = activeMoteSphere(sessionState, layout, visuals)
        val activeVisual = activeSphere?.let { visuals[it.id] }
        val activeFill = activeVisual?.fillLevel ?: 0f
        val fillDirection = activeVisual?.fillDirection ?: FillDirection.BottomToTop

        smokeSystem.update(activeSphere, activeFill, fillDirection, timeMs, layout.scale)

        if (activeSphere != null && activeFill > 0f) {
            val smokeColor = roleColor(activeSphere.role)
            smokeSystem.activeParticles().forEach { particle ->
                val r = particle.baseSize * layout.scale
                drawCircle(
                    color = smokeColor.copy(alpha = particle.alpha * 0.25f),
                    radius = r * 2.2f,
                    center = Offset(particle.x, particle.y),
                )
                drawCircle(
                    color = smokeColor.copy(alpha = particle.alpha * 0.55f),
                    radius = r * 1.2f,
                    center = Offset(particle.x, particle.y),
                )
                drawCircle(
                    color = smokeColor.copy(alpha = particle.alpha),
                    radius = r * 0.65f,
                    center = Offset(particle.x, particle.y),
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

private fun DrawScope.drawPipeSegment(
    from: Offset,
    to: Offset,
    scale: Float,
    pipePainter: androidx.compose.ui.graphics.painter.Painter,
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = kotlin.math.sqrt(dx * dx + dy * dy)
    if (length < 4f) return

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val w = 11f * scale.coerceIn(0.85f, 1.25f)

    drawLine(PipeMetal.copy(alpha = 0.35f), from, to, w * 1.35f, StrokeCap.Round)
    drawLine(PipeMetal, from, to, w, StrokeCap.Round)

    val midX = (from.x + to.x) / 2f
    val midY = (from.y + to.y) / 2f
    withTransform({
        translate(midX, midY)
        rotate(angle + 90f)
    }) {
        with(pipePainter) {
            draw(
                size = Size(w * 1.6f, length.coerceAtMost(size.height)),
                alpha = 0.35f,
            )
        }
    }
}

private fun DrawScope.drawBreathSphere(
    sphere: BreathSphere,
    visual: SphereVisualState,
    scale: Float,
    glassPainter: androidx.compose.ui.graphics.painter.Painter,
) {
    val roleColor = roleColor(sphere.role)
    val fill = visual.fillLevel.coerceIn(0f, 1f)
    val center = sphere.center
    val radius = sphere.radius
    val diameter = radius * 2f

    if (fill in 0.05f..0.99f) {
        drawCircle(
            color = roleColor.copy(alpha = 0.16f * fill),
            radius = radius * 1.4f,
            center = center,
        )
    }

    val circlePath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circlePath) {
        withTransform({
            translate(center.x - radius, center.y - radius)
        }) {
            with(glassPainter) {
                draw(size = Size(diameter, diameter), alpha = 0.75f)
            }
        }

        if (fill > 0f) {
            val fillRect = when (visual.fillDirection) {
                FillDirection.BottomToTop, FillDirection.BottomToTopHold -> {
                    val fillHeight = diameter * fill
                    Rect(
                        left = center.x - radius,
                        top = center.y + radius - fillHeight,
                        right = center.x + radius,
                        bottom = center.y + radius,
                    )
                }
                FillDirection.TopToBottom -> {
                    val fillHeight = diameter * fill
                    Rect(
                        left = center.x - radius,
                        top = center.y - radius,
                        right = center.x + radius,
                        bottom = center.y - radius + fillHeight,
                    )
                }
            }
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        roleColor.copy(alpha = 0.95f),
                        roleColor.copy(alpha = 0.8f),
                        roleColor.copy(alpha = 0.65f),
                    ),
                    startY = fillRect.top,
                    endY = fillRect.bottom,
                ),
                topLeft = Offset(fillRect.left, fillRect.top),
                size = Size(fillRect.width, fillRect.height),
            )
        }
    }

    drawCircle(
        color = GlassBorder.copy(alpha = 0.55f + 0.3f * fill),
        radius = radius,
        center = center,
        style = Stroke(width = (1.8f * scale).coerceAtLeast(1f)),
    )

    drawArc(
        color = Color.White.copy(alpha = 0.35f + 0.25f * fill),
        startAngle = 205f,
        sweepAngle = 75f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.78f, center.y - radius * 0.88f),
        size = Size(radius * 1.55f, radius * 1.15f),
        style = Stroke(width = (2f * scale).coerceAtLeast(1f), cap = StrokeCap.Round),
    )
}
