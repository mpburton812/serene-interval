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
import androidx.compose.ui.geometry.CornerRadius
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
import com.example.meditationparticles.ui.theme.GlassBorder
import com.example.meditationparticles.ui.theme.GlassTube
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import com.example.meditationparticles.ui.theme.SereneTertiary
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

private data class HourglassGeometry(
    val topBulb: Path,
    val bottomBulb: Path,
    val neckRect: Rect,
    val neckCenter: Offset,
    val sandColor: Color,
)

private data class FallingGrain(
    var x: Float,
    var y: Float,
    val size: Float,
    var speed: Float,
)

@Composable
fun HourglassCanvas(
    progress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    var timeMs by remember { mutableLongStateOf(0L) }
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    val grains = remember { List(24) { FallingGrain(0f, 0f, Random.nextFloat() * 2.5f + 1.5f, Random.nextFloat() * 2f + 2.5f) } }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nanos ->
                timeMs = nanos / 1_000_000L
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        canvasWidth = size.width
        canvasHeight = size.height

        val cx = size.width / 2f
        val maxW = size.width * 0.62f
        val maxH = size.height * 0.78f
        val bulbH = maxH * 0.42f
        val neckW = maxW * 0.12f
        val neckH = maxH * 0.08f
        val top = (size.height - maxH) / 2f

        val geometry = buildHourglassGeometry(
            cx = cx,
            top = top,
            bulbW = maxW,
            bulbH = bulbH,
            neckW = neckW,
            neckH = neckH,
        )

        drawAtmosphericGlow(cx, top + maxH / 2f)

        // Glass bulbs
        drawGlassBulb(geometry.topBulb)
        drawGlassBulb(geometry.bottomBulb)

        // Neck frame
        drawRoundRect(
            color = GlassTube,
            topLeft = Offset(geometry.neckRect.left, geometry.neckRect.top),
            size = Size(geometry.neckRect.width, geometry.neckRect.height),
            cornerRadius = CornerRadius(neckW / 2f),
        )
        drawRoundRect(
            color = GlassBorder.copy(alpha = 0.5f),
            topLeft = Offset(geometry.neckRect.left, geometry.neckRect.top),
            size = Size(geometry.neckRect.width, geometry.neckRect.height),
            cornerRadius = CornerRadius(neckW / 2f),
            style = Stroke(width = 2f),
        )

        val fill = progress.coerceIn(0f, 1f)
        val topFill = (1f - fill).coerceIn(0f, 1f)
        val bottomFill = fill.coerceIn(0f, 1f)

        // Sand in top bulb
        clipPath(geometry.topBulb) {
            drawSandLevel(
                bulbPath = geometry.topBulb,
                fillRatio = topFill,
                fromBottom = true,
                color = geometry.sandColor,
            )
        }

        // Sand in bottom bulb
        clipPath(geometry.bottomBulb) {
            drawSandLevel(
                bulbPath = geometry.bottomBulb,
                fillRatio = bottomFill,
                fromBottom = true,
                color = geometry.sandColor,
            )
        }

        // Falling grains through neck while running and sand remains
        if (isRunning && fill < 0.99f && topFill > 0.02f) {
            grains.forEachIndexed { index, grain ->
                if (grain.y <= 0f || grain.y >= geometry.neckRect.bottom + bulbH) {
                    grain.x = geometry.neckCenter.x + sin(timeMs / 180f + index) * neckW * 0.15f
                    grain.y = geometry.neckRect.top - grain.size
                    grain.speed = Random.nextFloat() * 2.5f + 2.5f
                }
                grain.y += grain.speed
                drawCircle(
                    color = geometry.sandColor.copy(alpha = 0.85f),
                    radius = grain.size,
                    center = Offset(grain.x, grain.y),
                )
            }
        }

        // Glass highlights
        drawBulbHighlight(geometry.topBulb)
        drawBulbHighlight(geometry.bottomBulb)
    }
}

private fun buildHourglassGeometry(
    cx: Float,
    top: Float,
    bulbW: Float,
    bulbH: Float,
    neckW: Float,
    neckH: Float,
): HourglassGeometry {
    val topBulb = bulbPath(
        left = cx - bulbW / 2f,
        top = top,
        width = bulbW,
        height = bulbH,
        taperBottom = true,
    )
    val bottomTop = top + bulbH + neckH
    val bottomBulb = bulbPath(
        left = cx - bulbW / 2f,
        top = bottomTop,
        width = bulbW,
        height = bulbH,
        taperBottom = false,
    )
    val neckRect = Rect(
        left = cx - neckW / 2f,
        top = top + bulbH,
        right = cx + neckW / 2f,
        bottom = top + bulbH + neckH,
    )
    return HourglassGeometry(
        topBulb = topBulb,
        bottomBulb = bottomBulb,
        neckRect = neckRect,
        neckCenter = Offset(cx, neckRect.center.y),
        sandColor = SereneTertiary.copy(alpha = 0.85f),
    )
}

private fun bulbPath(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    taperBottom: Boolean,
): Path {
    val path = Path()
    val cx = left + width / 2f
    if (taperBottom) {
        path.moveTo(left, top)
        path.quadraticTo(left, top + height * 0.55f, cx - width * 0.08f, top + height)
        path.lineTo(cx + width * 0.08f, top + height)
        path.quadraticTo(left + width, top + height * 0.55f, left + width, top)
        path.close()
    } else {
        path.moveTo(cx - width * 0.08f, top)
        path.lineTo(cx + width * 0.08f, top)
        path.quadraticTo(left + width, top + height * 0.45f, left + width, top + height)
        path.lineTo(left, top + height)
        path.quadraticTo(left, top + height * 0.45f, cx - width * 0.08f, top)
        path.close()
    }
    return path
}

private fun DrawScope.drawAtmosphericGlow(cx: Float, cy: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SereneSecondaryContainer.copy(alpha = 0.18f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.45f,
        ),
        radius = size.width * 0.45f,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawGlassBulb(path: Path) {
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f),
                GlassTube.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.12f),
            ),
        ),
    )
    drawPath(
        path = path,
        color = GlassBorder.copy(alpha = 0.55f),
        style = Stroke(width = 2.5f),
    )
}

private fun DrawScope.drawSandLevel(
    bulbPath: Path,
    fillRatio: Float,
    fromBottom: Boolean,
    color: Color,
) {
    if (fillRatio <= 0f) return
    val bounds = bulbPath.getBounds()
    val fillHeight = bounds.height * fillRatio
    val sandTop = if (fromBottom) bounds.bottom - fillHeight else bounds.top
    val sandRect = Rect(
        left = bounds.left + 4f,
        top = sandTop,
        right = bounds.right - 4f,
        bottom = bounds.bottom - 2f,
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                color.copy(alpha = 0.95f),
                color.copy(alpha = 0.75f),
                SerenePrimaryContainer.copy(alpha = 0.5f),
            ),
            startY = sandRect.top,
            endY = sandRect.bottom,
        ),
        topLeft = sandRect.topLeft,
        size = sandRect.size,
    )
    // Surface shimmer
    drawLine(
        color = Color.White.copy(alpha = 0.25f),
        start = Offset(sandRect.left + 8f, sandRect.top + 2f),
        end = Offset(sandRect.right - 8f, sandRect.top + 2f),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawBulbHighlight(path: Path) {
    val bounds = path.getBounds()
    drawArc(
        color = Color.White.copy(alpha = 0.35f),
        startAngle = 200f,
        sweepAngle = 80f,
        useCenter = false,
        topLeft = Offset(bounds.left + bounds.width * 0.08f, bounds.top + bounds.height * 0.1f),
        size = Size(bounds.width * 0.35f, bounds.height * 0.45f),
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
}
