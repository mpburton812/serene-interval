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

data class LivingTreeSphereFill(
    val colors: List<Color>,
    val isCenter: Boolean = false,
)

fun DrawScope.drawLivingTreeSphereInterior(
    center: Offset,
    radius: Float,
    fill: LivingTreeSphereFill,
    isDark: Boolean,
) {
    val circlePath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circlePath) {
        when {
            fill.colors.isEmpty() -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0A1218).copy(alpha = if (isDark) 0.55f else 0.42f),
                            Color(0xFF152028).copy(alpha = if (isDark) 0.35f else 0.28f),
                            Color(0xFF243038).copy(alpha = if (isDark) 0.18f else 0.14f),
                        ),
                        center = Offset(center.x, center.y + radius * 0.15f),
                        radius = radius * 1.05f,
                    ),
                    radius = radius,
                    center = center,
                )
                if (fill.isCenter) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.10f else 0.16f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius * 0.85f,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
            fill.colors.size == 1 -> {
                val color = fill.colors.first()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.96f),
                            color.copy(alpha = 0.78f),
                            color.copy(alpha = 0.62f),
                        ),
                        center = Offset(center.x, center.y + radius * 0.12f),
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            else -> {
                val sweep = 360f / fill.colors.size.coerceAtMost(4)
                fill.colors.take(4).forEachIndexed { index, color ->
                    val startAngle = -90f + index * sweep
                    drawArc(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.96f),
                                color.copy(alpha = 0.72f),
                            ),
                            center = center,
                            radius = radius,
                        ),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                    )
                }
            }
        }
    }
}

fun DrawScope.drawLivingTreeGlassSphere(
    center: Offset,
    radius: Float,
    fill: LivingTreeSphereFill,
    scale: Float,
    glassOverlay: androidx.compose.ui.graphics.painter.Painter?,
    isDark: Boolean,
    highlightAlpha: Float = 0.55f,
) {
    drawSphereDropShadow(center, radius, scale)
    drawLivingTreeSphereInterior(center, radius, fill, isDark)
    drawPhotoGlassSphereShell(
        center = center,
        radius = radius,
        scale = scale,
        glassOverlay = glassOverlay,
        highlightAlpha = highlightAlpha,
    )
}

fun DrawScope.drawLivingTreeSpoke(
    center: Offset,
    centerRadius: Float,
    nodeCenter: Offset,
    nodeRadius: Float,
    scale: Float,
    pipeTexture: androidx.compose.ui.graphics.painter.Painter?,
    shimmerPhase: Float,
) {
    val from = sphereEdgePoint(center, centerRadius, nodeCenter)
    val to = sphereEdgePoint(nodeCenter, nodeRadius, center)
    drawPhotoPipeSegment(
        from = from,
        to = to,
        scale = scale,
        pipeTexture = pipeTexture,
        shimmerPhase = shimmerPhase,
    )
}
