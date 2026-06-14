package com.example.meditationparticles.canvas

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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import com.example.meditationparticles.ui.theme.PipeMetal
import kotlin.math.atan2
import kotlin.math.sqrt

fun sphereEdgePoint(fromCenter: Offset, fromRadius: Float, towardCenter: Offset): Offset {
    val dx = towardCenter.x - fromCenter.x
    val dy = towardCenter.y - fromCenter.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    return Offset(
        fromCenter.x + dx / len * fromRadius,
        fromCenter.y + dy / len * fromRadius,
    )
}

fun DrawScope.drawSphereDropShadow(center: Offset, radius: Float, scale: Float) {
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.28f), Color.Transparent),
            center = Offset(center.x, center.y + radius * 0.92f),
            radius = radius * 0.95f,
        ),
        topLeft = Offset(center.x - radius * 0.82f, center.y + radius * 0.55f),
        size = Size(radius * 1.64f, radius * 0.42f),
    )
}

fun DrawScope.drawPhotoGlassSphereShell(
    center: Offset,
    radius: Float,
    scale: Float,
    glassOverlay: Painter?,
    highlightAlpha: Float = 0.55f,
) {
    val diameter = radius * 2f
    val circlePath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circlePath) {
        glassOverlay?.let { overlay ->
            withTransform({ translate(center.x - radius, center.y - radius) }) {
                with(overlay) {
                    draw(size = Size(diameter, diameter), alpha = 0.92f)
                }
            }
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.38f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
        style = Stroke(width = (2.4f * scale).coerceAtLeast(1.2f)),
    )

    drawArc(
        color = Color.White.copy(alpha = highlightAlpha),
        startAngle = 205f,
        sweepAngle = 78f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.9f),
        size = Size(radius * 1.62f, radius * 1.18f),
        style = Stroke(width = (2.6f * scale).coerceAtLeast(1.2f), cap = StrokeCap.Round),
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.42f),
        radius = radius * 0.08f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.34f),
    )
}

fun DrawScope.drawPhotoPipeSegment(
    from: Offset,
    to: Offset,
    scale: Float,
    pipeTexture: Painter?,
    shimmerPhase: Float = 0f,
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 4f) return

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    val w = 12.5f * scale.coerceIn(0.85f, 1.25f)
    val nx = dx / length
    val ny = dy / length
    val capInset = w * 0.52f
    val start = Offset(from.x + nx * capInset, from.y + ny * capInset)
    val end = Offset(to.x - nx * capInset, to.y - ny * capInset)
    val segLen = sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y))
    if (segLen < 4f) return

    val perpX = -ny
    val perpY = nx
    val shimmerOffset = shimmerPhase * 0.18f

    drawLine(
        color = Color.Black.copy(alpha = 0.22f),
        start = Offset(start.x + perpX * 1.5f, start.y + perpY * 1.5f),
        end = Offset(end.x + perpX * 1.5f, end.y + perpY * 1.5f),
        strokeWidth = w * 1.45f,
        cap = StrokeCap.Round,
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(
                PipeMetal.copy(alpha = 0.50f + shimmerOffset),
                Color(0xFF9AA8B2).copy(alpha = 0.85f + shimmerOffset * 0.5f),
                Color(0xFFE8EDF0).copy(alpha = 0.95f),
                Color(0xFF9AA8B2).copy(alpha = 0.85f + shimmerOffset * 0.5f),
                PipeMetal.copy(alpha = 0.60f + shimmerOffset),
            ),
            start = Offset(start.x - perpX * w, start.y - perpY * w),
            end = Offset(start.x + perpX * w, start.y + perpY * w),
        ),
        start = start,
        end = end,
        strokeWidth = w,
        cap = StrokeCap.Round,
    )

    val midX = (start.x + end.x) / 2f
    val midY = (start.y + end.y) / 2f
    val canvasHeight = size.height
    pipeTexture?.let { texture ->
        withTransform({
            translate(midX, midY)
            rotate(angle + 90f)
        }) {
            with(texture) {
                draw(
                    size = Size(w * 1.75f, segLen.coerceAtMost(canvasHeight)),
                    alpha = 0.68f + shimmerOffset * 0.35f,
                )
            }
        }
    }

    listOf(start, end).forEach { joint ->
        if (segLen < w * 1.8f) return@forEach
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFDCE3E8), Color(0xFF6E7A84), Color(0xFF3A4248)),
                center = joint,
                radius = w * 0.72f,
            ),
            radius = w * 0.72f,
            center = joint,
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.30f + shimmerOffset),
            radius = w * 0.22f,
            center = Offset(joint.x - perpX * w * 0.18f, joint.y - perpY * w * 0.18f),
        )
    }
}
