package com.example.meditationparticles.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
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
    val frameTop: Rect,
    val frameBottom: Rect,
    val sandColor: Color,
    val bulbH: Float,
)

private data class FallingGrain(
    var x: Float,
    var y: Float,
    val size: Float,
    var speed: Float,
)

private val FrameWoodDark = Color(0xFF3D2E1F)
private val FrameWoodMid = Color(0xFF6B4E2E)
private val FrameWoodLight = Color(0xFF9A7348)
private val FrameWoodHighlight = Color(0xFFC4A06A)
private val NeckMetalDark = Color(0xFF4A5258)
private val NeckMetalLight = Color(0xFFB8C4CC)
private val SandDeep = Color(0xFFC47A2A)
private val SandMid = Color(0xFFE8A84C)
private val SandHighlight = Color(0xFFF5D08A)

@Composable
fun HourglassCanvas(
    progress: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
) {
    var timeMs by remember { mutableLongStateOf(0L) }
    val grains = remember {
        List(28) {
            FallingGrain(
                x = 0f,
                y = 0f,
                size = Random.nextFloat() * 2f + 1.2f,
                speed = Random.nextFloat() * 2f + 2.5f,
            )
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { nanos ->
                timeMs = nanos / 1_000_000L
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas

        val cx = size.width / 2f
        val maxW = size.width * 0.58f
        val maxH = size.height * 0.82f
        val frameH = maxW * 0.09f
        val bulbH = maxH * 0.40f
        val neckW = maxW * 0.11f
        val neckH = maxH * 0.07f
        val top = (size.height - maxH) / 2f + frameH * 0.35f

        val geometry = buildHourglassGeometry(
            cx = cx,
            top = top,
            bulbW = maxW,
            bulbH = bulbH,
            neckW = neckW,
            neckH = neckH,
            frameH = frameH,
        )

        drawHourglassShadow(cx, top + maxH * 0.92f, maxW)

        drawAtmosphericGlow(cx, top + maxH / 2f)

        drawWoodFrame(geometry.frameTop)
        drawWoodFrame(geometry.frameBottom)

        drawPhotoGlassInterior(geometry.topBulb)
        drawPhotoGlassInterior(geometry.bottomBulb)

        val fill = progress.coerceIn(0f, 1f)
        val topFill = (1f - fill).coerceIn(0f, 1f)
        val bottomFill = fill.coerceIn(0f, 1f)

        clipPath(geometry.topBulb) {
            drawSandPile(
                bulbPath = geometry.topBulb,
                fillRatio = topFill,
                pileFromBottom = true,
                sandColor = geometry.sandColor,
            )
        }

        clipPath(geometry.bottomBulb) {
            drawSandPile(
                bulbPath = geometry.bottomBulb,
                fillRatio = bottomFill,
                pileFromBottom = true,
                sandColor = geometry.sandColor,
            )
        }

        drawPhotoNeck(geometry.neckRect, neckW)

        if (isRunning && fill < 0.99f && topFill > 0.02f) {
            grains.forEachIndexed { index, grain ->
                if (grain.y <= 0f || grain.y >= geometry.neckRect.bottom + bulbH) {
                    grain.x = geometry.neckCenter.x + sin(timeMs / 160f + index * 0.7f) * neckW * 0.12f
                    grain.y = geometry.neckRect.top - grain.size
                    grain.speed = Random.nextFloat() * 2.5f + 2.8f
                }
                grain.y += grain.speed
                drawFallingGrain(Offset(grain.x, grain.y), grain.size, geometry.sandColor)
            }
        }

        drawPhotoGlassShell(geometry.topBulb)
        drawPhotoGlassShell(geometry.bottomBulb)
    }
}

private fun buildHourglassGeometry(
    cx: Float,
    top: Float,
    bulbW: Float,
    bulbH: Float,
    neckW: Float,
    neckH: Float,
    frameH: Float,
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
    val frameW = bulbW * 1.08f
    return HourglassGeometry(
        topBulb = topBulb,
        bottomBulb = bottomBulb,
        neckRect = neckRect,
        neckCenter = Offset(cx, neckRect.center.y),
        frameTop = Rect(
            left = cx - frameW / 2f,
            top = top - frameH * 0.55f,
            right = cx + frameW / 2f,
            bottom = top + frameH * 0.45f,
        ),
        frameBottom = Rect(
            left = cx - frameW / 2f,
            top = bottomTop + bulbH - frameH * 0.45f,
            right = cx + frameW / 2f,
            bottom = bottomTop + bulbH + frameH * 0.55f,
        ),
        sandColor = SereneTertiary,
        bulbH = bulbH,
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
        path.quadraticTo(left, top + height * 0.52f, cx - width * 0.07f, top + height)
        path.lineTo(cx + width * 0.07f, top + height)
        path.quadraticTo(left + width, top + height * 0.52f, left + width, top)
        path.close()
    } else {
        path.moveTo(cx - width * 0.07f, top)
        path.lineTo(cx + width * 0.07f, top)
        path.quadraticTo(left + width, top + height * 0.48f, left + width, top + height)
        path.lineTo(left, top + height)
        path.quadraticTo(left, top + height * 0.48f, cx - width * 0.07f, top)
        path.close()
    }
    return path
}

private fun DrawScope.drawHourglassShadow(cx: Float, baseY: Float, width: Float) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent),
            center = Offset(cx, baseY),
            radius = width * 0.55f,
        ),
        topLeft = Offset(cx - width * 0.48f, baseY - width * 0.06f),
        size = Size(width * 0.96f, width * 0.14f),
    )
}

private fun DrawScope.drawAtmosphericGlow(cx: Float, cy: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                SereneSecondaryContainer.copy(alpha = 0.22f),
                SerenePrimaryContainer.copy(alpha = 0.08f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.48f,
        ),
        radius = size.width * 0.48f,
        center = Offset(cx, cy),
    )
}

private fun DrawScope.drawWoodFrame(frame: Rect) {
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(FrameWoodHighlight, FrameWoodMid, FrameWoodDark),
            startY = frame.top,
            endY = frame.bottom,
        ),
        topLeft = frame.topLeft,
        size = frame.size,
        cornerRadius = CornerRadius(frame.height * 0.22f),
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.18f),
            ),
            start = Offset(frame.left, frame.top),
            end = Offset(frame.right, frame.bottom),
        ),
        topLeft = frame.topLeft,
        size = frame.size,
        cornerRadius = CornerRadius(frame.height * 0.22f),
    )
    drawRoundRect(
        color = FrameWoodDark.copy(alpha = 0.55f),
        topLeft = frame.topLeft,
        size = frame.size,
        cornerRadius = CornerRadius(frame.height * 0.22f),
        style = Stroke(width = 1.5f),
    )
    val inset = frame.height * 0.18f
    drawLine(
        color = FrameWoodLight.copy(alpha = 0.45f),
        start = Offset(frame.left + inset, frame.top + inset * 0.6f),
        end = Offset(frame.right - inset, frame.top + inset * 0.6f),
        strokeWidth = 1.2f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPhotoGlassInterior(path: Path) {
    val bounds = path.getBounds()
    clipPath(path) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A1218).copy(alpha = 0.62f),
                    Color(0xFF152028).copy(alpha = 0.38f),
                    Color(0xFF243038).copy(alpha = 0.15f),
                ),
                center = Offset(bounds.center.x, bounds.center.y + bounds.height * 0.12f),
                radius = bounds.width * 0.72f,
            ),
        )
    }
}

private fun DrawScope.drawPhotoGlassShell(path: Path) {
    val bounds = path.getBounds()
    val cx = bounds.center.x
    val cy = bounds.center.y

    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.42f),
                GlassTube.copy(alpha = 0.18f),
                Color.White.copy(alpha = 0.08f),
                GlassTube.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.22f),
            ),
            start = Offset(bounds.left, bounds.top),
            end = Offset(bounds.right, bounds.bottom),
        ),
        alpha = 0.75f,
    )

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.35f)),
            center = Offset(cx, cy),
            radius = bounds.width * 0.55f,
        ),
        alpha = 0.85f,
    )

    drawPath(
        path = path,
        color = GlassBorder.copy(alpha = 0.65f),
        style = Stroke(width = 2.8f),
    )

    drawArc(
        color = Color.White.copy(alpha = 0.58f),
        startAngle = 205f,
        sweepAngle = 72f,
        useCenter = false,
        topLeft = Offset(bounds.left + bounds.width * 0.1f, bounds.top + bounds.height * 0.12f),
        size = Size(bounds.width * 0.32f, bounds.height * 0.42f),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round),
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.48f),
        radius = bounds.width * 0.028f,
        center = Offset(bounds.left + bounds.width * 0.28f, bounds.top + bounds.height * 0.22f),
    )

    drawLine(
        color = Color.White.copy(alpha = 0.15f),
        start = Offset(bounds.right - bounds.width * 0.12f, bounds.top + bounds.height * 0.18f),
        end = Offset(bounds.right - bounds.width * 0.08f, bounds.bottom - bounds.height * 0.15f),
        strokeWidth = 1.5f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPhotoNeck(neckRect: Rect, neckW: Float) {
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(NeckMetalLight, NeckMetalDark, NeckMetalLight),
            start = Offset(neckRect.left, neckRect.center.y),
            end = Offset(neckRect.right, neckRect.center.y),
        ),
        topLeft = neckRect.topLeft,
        size = neckRect.size,
        cornerRadius = CornerRadius(neckW / 2f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.35f),
        topLeft = Offset(neckRect.left + 1f, neckRect.top + 1f),
        size = Size(neckRect.width - 2f, neckRect.height * 0.35f),
        cornerRadius = CornerRadius(neckW / 2f),
    )
    drawRoundRect(
        color = GlassBorder.copy(alpha = 0.5f),
        topLeft = neckRect.topLeft,
        size = neckRect.size,
        cornerRadius = CornerRadius(neckW / 2f),
        style = Stroke(width = 1.8f),
    )
}

private fun DrawScope.drawSandPile(
    bulbPath: Path,
    fillRatio: Float,
    pileFromBottom: Boolean,
    sandColor: Color,
) {
    if (fillRatio <= 0f) return
    val bounds = bulbPath.getBounds()
    val cx = bounds.center.x
    val sandHeight = bounds.height * fillRatio.coerceIn(0f, 1f)
    val sandBottom = if (pileFromBottom) bounds.bottom - 3f else bounds.top + sandHeight
    val sandTop = if (pileFromBottom) sandBottom - sandHeight else bounds.top
    val pileWidth = bounds.width * (0.55f + 0.38f * fillRatio.coerceIn(0.15f, 1f))
    val meniscusLift = bounds.height * 0.04f * fillRatio.coerceIn(0f, 1f)

    val sandPath = Path().apply {
        if (pileFromBottom) {
            moveTo(bounds.left + 6f, sandBottom)
            lineTo(bounds.right - 6f, sandBottom)
            lineTo(bounds.right - 6f, sandTop + meniscusLift)
            quadraticTo(cx, sandTop - meniscusLift * 0.8f, bounds.left + 6f, sandTop + meniscusLift)
            close()
        } else {
            moveTo(bounds.left + 6f, sandTop)
            lineTo(bounds.right - 6f, sandTop)
            lineTo(cx + pileWidth / 2f, sandBottom)
            lineTo(cx - pileWidth / 2f, sandBottom)
            close()
        }
    }

    drawPath(
        sandPath,
        brush = Brush.verticalGradient(
            colors = listOf(SandHighlight, SandMid, SandDeep, sandColor.copy(alpha = 0.9f)),
            startY = sandTop,
            endY = sandBottom,
        ),
    )

    drawPath(
        sandPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.1f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.14f),
            ),
            start = Offset(bounds.left, bounds.center.y),
            end = Offset(bounds.right, bounds.center.y),
        ),
        alpha = 0.7f,
    )

    val surfaceY = if (pileFromBottom) sandTop + meniscusLift else sandTop
    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.55f),
                Color.Transparent,
            ),
            startX = cx - pileWidth * 0.35f,
            endX = cx + pileWidth * 0.35f,
        ),
        start = Offset(cx - pileWidth * 0.35f, surfaceY),
        end = Offset(cx + pileWidth * 0.35f, surfaceY),
        strokeWidth = 2.2f,
        cap = StrokeCap.Round,
    )

    if (fillRatio > 0.08f) {
        val grainCount = (fillRatio * 12).toInt().coerceIn(3, 12)
        repeat(grainCount) { i ->
            val gx = cx + (i - grainCount / 2f) * (pileWidth / grainCount) * 0.9f
            val gy = surfaceY + (i % 3) * 2.5f + 3f
            drawCircle(
                color = SandDeep.copy(alpha = 0.35f),
                radius = 1.1f + (i % 2) * 0.4f,
                center = Offset(gx, gy),
            )
        }
    }
}

private fun DrawScope.drawFallingGrain(center: Offset, radius: Float, sandColor: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sandColor.copy(alpha = 0.18f), Color.Transparent),
            center = center,
            radius = radius * 3.2f,
        ),
        radius = radius * 3.2f,
        center = center,
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(SandHighlight.copy(alpha = 0.92f), sandColor.copy(alpha = 0.75f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
