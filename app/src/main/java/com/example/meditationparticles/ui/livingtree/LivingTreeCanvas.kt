package com.example.meditationparticles.ui.livingtree

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import com.example.meditationparticles.domain.livingtree.LivingTreeLayout
import kotlin.math.hypot

data class LivingTreeGraphNode(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val radius: Float,
    val bubbleColors: List<Color>,
    val isFilteredMatch: Boolean,
)

@Composable
fun LivingTreeCanvas(
    centerLabel: String,
    nodes: List<LivingTreeGraphNode>,
    onPersonTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
    filterActive: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val pulseTransition = rememberInfiniteTransition(label = "center_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(nodes) {
                detectTapGestures { offset ->
                    nodes.firstOrNull { node ->
                        hypot(
                            (offset.x - node.x).toDouble(),
                            (offset.y - node.y).toDouble(),
                        ) <= node.radius
                    }?.let { onPersonTap(it.id) }
                }
            },
    ) {
        if (nodes.isEmpty()) return@Canvas
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val centerRadius = LivingTreeLayout.computeCenterRadius(minOf(size.width, size.height)) * pulseScale
        val lineColor = Color.White.copy(alpha = 0.28f)

        nodes.forEach { node ->
            drawLine(
                color = lineColor,
                start = Offset(centerX, centerY),
                end = Offset(node.x, node.y),
                strokeWidth = 1.5f,
            )
        }

        nodes.forEach { node ->
            drawPersonBubble(
                center = Offset(node.x, node.y),
                radius = node.radius,
                colors = if (filterActive && node.isFilteredMatch) node.bubbleColors else emptyList(),
            )
            drawLabel(
                textMeasurer = textMeasurer,
                text = LivingTreeDefaults.truncateName(node.name),
                center = Offset(node.x, node.y + node.radius + 14f),
            )
        }

        drawPersonBubble(
            center = Offset(centerX, centerY),
            radius = centerRadius,
            colors = emptyList(),
            isCenter = true,
        )
        drawLabel(
            textMeasurer = textMeasurer,
            text = LivingTreeDefaults.truncateName(centerLabel, maxLength = 16),
            center = Offset(centerX, centerY),
            onBubble = true,
        )
    }
}

private fun DrawScope.drawPersonBubble(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    isCenter: Boolean = false,
) {
    when {
        colors.isEmpty() -> {
            drawCircle(
                color = Color.White.copy(alpha = if (isCenter) 0.22f else 0.14f),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = Color.White.copy(alpha = if (isCenter) 0.55f else 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = if (isCenter) 2.5f else 1.5f),
            )
        }
        colors.size == 1 -> {
            drawCircle(color = colors.first().copy(alpha = 0.85f), radius = radius, center = center)
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f),
            )
        }
        else -> drawSplitBubble(center, radius, colors.take(4))
    }
}

private fun DrawScope.drawSplitBubble(center: Offset, radius: Float, colors: List<Color>) {
    val sweep = 360f / colors.size
    colors.forEachIndexed { index, color ->
        val startAngle = -90f + index * sweep
        drawArc(
            color = color.copy(alpha = 0.88f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        )
    }
    drawCircle(
        color = Color.White.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f),
    )
}

private fun DrawScope.drawLabel(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    onBubble: Boolean = false,
) {
    val style = TextStyle(
        color = Color.White.copy(alpha = 0.92f),
        fontSize = if (onBubble) 13.sp else 11.sp,
        textAlign = TextAlign.Center,
    )
    val layout = textMeasurer.measure(text, style)
    val topLeft = Offset(
        x = center.x - layout.size.width / 2f,
        y = center.y - layout.size.height / 2f,
    )
    drawText(layout, topLeft = topLeft)
}
