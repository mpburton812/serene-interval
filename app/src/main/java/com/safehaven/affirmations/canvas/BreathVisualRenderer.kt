package com.safehaven.affirmations.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import com.safehaven.affirmations.domain.breathing.FillDirection
import com.safehaven.affirmations.ui.theme.BreathSandExhale
import com.safehaven.affirmations.ui.theme.BreathSandHold
import com.safehaven.affirmations.ui.theme.BreathSandInhale
import com.safehaven.affirmations.ui.theme.BreathStartStar
import com.safehaven.affirmations.ui.theme.PipeMetal
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal fun roleColor(role: SphereRole): Color = when (role) {
    SphereRole.InhaleBlue -> BreathSandInhale
    SphereRole.ExhaleRed -> BreathSandExhale
    SphereRole.HoldPurple -> BreathSandHold
}

internal fun DrawScope.drawBreathingStructure(
    layout: BreathStructureLayout,
    visuals: Map<Int, SphereVisualState>,
    startSphereId: Int?,
    glassOverlay: Painter?,
    pipeTexture: Painter?,
) {
    layout.allSpheres.forEach { sphere ->
        drawSphereDropShadow(sphere.center, sphere.radius, layout.scale)
    }

    if (layout.pipes.isNotEmpty()) {
        layout.pipes.forEach { pipe ->
            val from = layout.sphere(pipe.fromSphereId) ?: return@forEach
            val to = layout.sphere(pipe.toSphereId) ?: return@forEach
            val (a, b) = layout.pipeEdgePoints(pipe) ?: return@forEach
            val endpointClip = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(
                    Rect(
                        from.center.x - from.radius,
                        from.center.y - from.radius,
                        from.center.x + from.radius,
                        from.center.y + from.radius,
                    ),
                )
                addOval(
                    Rect(
                        to.center.x - to.radius,
                        to.center.y - to.radius,
                        to.center.x + to.radius,
                        to.center.y + to.radius,
                    ),
                )
            }
            clipPath(endpointClip) {
                drawPhotoPipeSegment(a, b, layout.scale, pipeTexture)
            }
        }
    }

    layout.allSpheres.forEach { sphere ->
        val visual = visuals[sphere.id] ?: SphereVisualState(0f, false)
        drawPhotoGlassSphereInterior(
            sphere = sphere,
            visual = visual,
        )
    }
}

internal fun DrawScope.drawBreathingStructureGlass(
    layout: BreathStructureLayout,
    visuals: Map<Int, SphereVisualState>,
    startSphereId: Int?,
    glassOverlay: Painter?,
    scale: Float,
) {
    layout.allSpheres.forEach { sphere ->
        val visual = visuals[sphere.id] ?: SphereVisualState(0f, false)
        drawPhotoGlassSphereShell(
            center = sphere.center,
            radius = sphere.radius,
            scale = scale,
            glassOverlay = glassOverlay,
            highlightAlpha = 0.55f + 0.25f * visual.fillLevel.coerceIn(0f, 1f),
        )
        if (sphere.id == startSphereId) {
            drawStartStar(sphere.center, sphere.radius, scale)
        }
    }
}

internal fun DrawScope.drawPhotoGlassSphereInterior(
    sphere: BreathSphere,
    visual: SphereVisualState,
) {
    val roleColor = roleColor(sphere.role)
    val fill = visual.fillLevel.coerceIn(0f, 1f)
    val center = sphere.center
    val radius = sphere.radius

    if (fill in 0.04f..0.99f) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(roleColor.copy(alpha = 0.22f * fill), Color.Transparent),
                center = center,
                radius = radius * 1.35f,
            ),
            radius = radius * 1.35f,
            center = center,
        )
    }

    val circlePath = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circlePath) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A1218).copy(alpha = 0.55f),
                    Color(0xFF152028).copy(alpha = 0.35f),
                    Color(0xFF243038).copy(alpha = 0.18f),
                ),
                center = Offset(center.x, center.y + radius * 0.15f),
                radius = radius * 1.05f,
            ),
            radius = radius,
            center = center,
        )

        if (fill > 0f) {
            drawLiquidFill(center, radius, fill, visual.liquidDirection(), roleColor)
        }
    }
}

private fun DrawScope.drawLiquidFill(
    center: Offset,
    radius: Float,
    fill: Float,
    direction: FillDirection,
    roleColor: Color,
) {
    val diameter = radius * 2f
    val interfaceY = when (direction) {
        FillDirection.BottomToTop, FillDirection.BottomToTopHold ->
            center.y + radius - fill * diameter
        FillDirection.TopToBottom ->
            center.y - radius + fill * diameter
    }
    val meniscusBulge = radius * 0.07f * if (direction == FillDirection.TopToBottom) -1f else 1f
    val left = center.x - radius
    val right = center.x + radius

    val fillPath = Path().apply {
        when (direction) {
            FillDirection.BottomToTop, FillDirection.BottomToTopHold -> {
                moveTo(left, center.y + radius)
                lineTo(right, center.y + radius)
                lineTo(right, interfaceY)
                quadraticTo(center.x, interfaceY - meniscusBulge, left, interfaceY)
                close()
            }
            FillDirection.TopToBottom -> {
                moveTo(left, center.y - radius)
                lineTo(right, center.y - radius)
                lineTo(right, interfaceY)
                quadraticTo(center.x, interfaceY - meniscusBulge, left, interfaceY)
                close()
            }
        }
    }

    drawPath(
        fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                roleColor.copy(alpha = 0.98f),
                roleColor.copy(alpha = 0.82f),
                roleColor.copy(alpha = 0.68f),
            ),
            startY = when (direction) {
                FillDirection.BottomToTop, FillDirection.BottomToTopHold -> interfaceY
                FillDirection.TopToBottom -> center.y - radius
            },
            endY = when (direction) {
                FillDirection.BottomToTop, FillDirection.BottomToTopHold -> center.y + radius
                FillDirection.TopToBottom -> interfaceY
            },
        ),
    )

    drawPath(
        fillPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.12f),
            ),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
        ),
        alpha = 0.65f,
    )

    drawLine(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.75f),
                Color.White.copy(alpha = 0.55f),
                Color.Transparent,
            ),
            startX = left + radius * 0.15f,
            endX = right - radius * 0.15f,
        ),
        start = Offset(left + radius * 0.15f, interfaceY - meniscusBulge * 0.35f),
        end = Offset(right - radius * 0.15f, interfaceY - meniscusBulge * 0.35f),
        strokeWidth = (radius * 0.09f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round,
    )
}

internal fun DrawScope.drawSmokeParticle(
    particle: SmokeParticle,
    roleColor: Color,
    scale: Float,
) {
    val core = roleColor.copy(alpha = particle.alpha * 0.85f)
    val halo = roleColor.copy(alpha = particle.alpha * 0.28f)
    val wisp = roleColor.copy(alpha = particle.alpha * 0.12f)
    val center = Offset(particle.x, particle.y)
    val r = particle.size * scale

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(wisp, Color.Transparent),
            center = center,
            radius = r * 3.6f,
        ),
        radius = r * 3.6f,
        center = center,
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(halo, Color.Transparent),
            center = center,
            radius = r * 2.1f,
        ),
        radius = r * 2.1f,
        center = center,
        blendMode = BlendMode.Plus,
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(core, roleColor.copy(alpha = particle.alpha * 0.35f), Color.Transparent),
            center = center,
            radius = r * 1.05f,
        ),
        radius = r * 1.05f,
        center = center,
        blendMode = BlendMode.Plus,
    )
}

internal fun DrawScope.drawStartStar(center: Offset, sphereRadius: Float, scale: Float) {
    val outerRadius = sphereRadius * 0.36f
    val innerRadius = outerRadius * 0.45f
    val points = 5
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = PI / 2.0 + i * PI / points
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y - (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, BreathStartStar)
    drawPath(
        path,
        Color(0xFFFF8F00).copy(alpha = 0.65f),
        style = Stroke(width = (1.6f * scale).coerceAtLeast(1f)),
    )
}

internal fun fillInterfaceY(
    center: Offset,
    radius: Float,
    fill: Float,
    direction: FillDirection,
): Float {
    val diameter = radius * 2f
    return when (direction) {
        FillDirection.BottomToTop, FillDirection.BottomToTopHold ->
            center.y + radius - fill * diameter
        FillDirection.TopToBottom ->
            center.y - radius + fill * diameter
    }
}
