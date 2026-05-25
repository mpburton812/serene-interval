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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import android.graphics.BitmapFactory
import com.example.meditationparticles.R
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import com.example.meditationparticles.ui.theme.SereneTertiary
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class HourglassGeometry(
    val topBulb: Path,
    val bottomBulb: Path,
    val neckRect: Rect,
    val neckCenter: Offset,
    val sandColor: Color,
    val bulbH: Float,
)

private data class FallingGrain(
    var x: Float,
    var y: Float,
    val size: Float,
    var speed: Float,
)

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
    val context = LocalContext.current
    val backPainter = painterResource(R.drawable.hourglass_back)
    val frontPainter = painterResource(R.drawable.hourglass_front)
    val maskBitmap = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.hourglass_mask).asImageBitmap()
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

        val fitRect = computeContentScaleFitRect(
            srcWidth = maskBitmap.width.toFloat(),
            srcHeight = maskBitmap.height.toFloat(),
            dstWidth = size.width,
            dstHeight = size.height,
        )
        if (fitRect.width <= 0f || fitRect.height <= 0f) return@Canvas

        val geometry = buildSandGeometry(fitRect.width, fitRect.height)

        drawHourglassShadow(fitRect.center.x, fitRect.bottom - fitRect.height * 0.04f, fitRect.width)
        drawAtmosphericGlow(fitRect.center.x, fitRect.center.y)

        withTransform({ translate(fitRect.left, fitRect.top) }) {
            with(backPainter) {
                draw(size = fitRect.size)
            }
        }

        drawContext.canvas.saveLayer(fitRect, Paint())
        withTransform({ translate(fitRect.left, fitRect.top) }) {
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

            if (isRunning && fill < 0.99f && topFill > 0.02f) {
                grains.forEachIndexed { index, grain ->
                    if (grain.y <= 0f || grain.y >= geometry.neckRect.bottom + geometry.bulbH) {
                        grain.x = geometry.neckCenter.x +
                            sin(timeMs / 160f + index * 0.7f) * geometry.neckRect.width * 0.12f
                        grain.y = geometry.neckRect.top - grain.size
                        grain.speed = Random.nextFloat() * 2.5f + 2.8f
                    }
                    grain.y += grain.speed
                    drawFallingGrain(Offset(grain.x, grain.y), grain.size, geometry.sandColor)
                }
            }
        }
        drawImage(
            image = maskBitmap,
            dstOffset = IntOffset(fitRect.left.roundToInt(), fitRect.top.roundToInt()),
            dstSize = IntSize(fitRect.width.roundToInt(), fitRect.height.roundToInt()),
            blendMode = BlendMode.DstIn,
        )
        drawContext.canvas.restore()

        withTransform({ translate(fitRect.left, fitRect.top) }) {
            with(frontPainter) {
                draw(size = fitRect.size)
            }
        }
    }
}

private fun computeContentScaleFitRect(
    srcWidth: Float,
    srcHeight: Float,
    dstWidth: Float,
    dstHeight: Float,
): Rect {
    if (srcWidth <= 0f || srcHeight <= 0f) return Rect.Zero
    val scale = min(dstWidth / srcWidth, dstHeight / srcHeight)
    val width = srcWidth * scale
    val height = srcHeight * scale
    val left = (dstWidth - width) / 2f
    val top = (dstHeight - height) / 2f
    return Rect(left, top, left + width, top + height)
}

private fun buildSandGeometry(imageWidth: Float, imageHeight: Float): HourglassGeometry {
    val cx = imageWidth / 2f
    val bulbW = imageWidth * 0.72f
    val frameInset = imageHeight * 0.07f
    val bulbH = imageHeight * 0.36f
    val neckW = imageWidth * 0.14f
    val neckH = imageHeight * 0.06f
    val top = frameInset * 0.85f

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
            colors = listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
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
                SereneSecondaryContainer.copy(alpha = 0.18f),
                SerenePrimaryContainer.copy(alpha = 0.06f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = size.width * 0.42f,
        ),
        radius = size.width * 0.42f,
        center = Offset(cx, cy),
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
