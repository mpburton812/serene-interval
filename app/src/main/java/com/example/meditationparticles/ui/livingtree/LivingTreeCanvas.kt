package com.example.meditationparticles.ui.livingtree

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.meditationparticles.domain.livingtree.LivingTreeTextContrast
import com.example.meditationparticles.ui.theme.isDarkScheme
import kotlin.math.atan2
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
    centerRadius: Float,
    onPersonTap: (Long) -> Unit,
    modifier: Modifier = Modifier,
    orbitRadius: Float = 0f,
    filterActive: Boolean = false,
    onPersonDragAngle: ((Long, Double) -> Unit)? = null,
    onPersonDragEnd: ((Long, Double) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme(scheme)
    val labelColors = LivingTreeTextContrast.canvasLabelColors(scheme, isDark)
    val bubbleBackdrop = Color.Black.copy(alpha = 0.8f)
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
            .pointerInput(nodes, orbitRadius, onPersonDragAngle, onPersonDragEnd) {
                if (onPersonDragAngle != null && onPersonDragEnd != null && orbitRadius > 0f) {
                    var draggingId: Long? = null
                    var dragMoved = false
                    var lastAngle = 0.0

                    detectDragGestures(
                        onDragStart = { offset ->
                            draggingId = nodes.firstOrNull { node ->
                                hypot(
                                    (offset.x - node.x).toDouble(),
                                    (offset.y - node.y).toDouble(),
                                ) <= node.radius
                            }?.id
                            dragMoved = false
                        },
                        onDrag = { change, _ ->
                            val id = draggingId ?: return@detectDragGestures
                            dragMoved = true
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            lastAngle = atan2(
                                (change.position.y - centerY).toDouble(),
                                (change.position.x - centerX).toDouble(),
                            )
                            onPersonDragAngle(id, lastAngle)
                            change.consume()
                        },
                        onDragEnd = {
                            draggingId?.let { id ->
                                if (dragMoved) {
                                    onPersonDragEnd(id, lastAngle)
                                }
                            }
                            draggingId = null
                        },
                        onDragCancel = {
                            draggingId = null
                        },
                    )
                }
            }
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
        val pulsedCenterRadius = centerRadius * pulseScale
        val lineColor = if (isDark) {
            Color.White.copy(alpha = 0.28f)
        } else {
            scheme.onSurface.copy(alpha = 0.22f)
        }

        nodes.forEach { node ->
            drawLine(
                color = lineColor,
                start = Offset(centerX, centerY),
                end = Offset(node.x, node.y),
                strokeWidth = 1.5f,
            )
        }

        nodes.forEach { node ->
            val colors = if (filterActive && node.isFilteredMatch) node.bubbleColors else emptyList()
            drawPersonBubble(
                center = Offset(node.x, node.y),
                radius = node.radius,
                colors = colors,
                backdropColor = bubbleBackdrop,
            )
            drawLabel(
                textMeasurer = textMeasurer,
                text = LivingTreeDefaults.truncateName(node.name),
                center = Offset(node.x, node.y + node.radius + 16f),
                labelColors = labelColors,
                fontSizeSp = 11f,
                showPlate = true,
            )
        }

        drawPersonBubble(
            center = Offset(centerX, centerY),
            radius = pulsedCenterRadius,
            colors = emptyList(),
            isCenter = true,
            isDark = isDark,
            backdropColor = bubbleBackdrop,
        )
        drawLabel(
            textMeasurer = textMeasurer,
            text = LivingTreeDefaults.truncateName(centerLabel, maxLength = 16),
            center = Offset(centerX, centerY),
            labelColors = labelColors,
            fontSizeSp = 13f,
            showPlate = true,
            onBubble = true,
        )
    }
}

private fun DrawScope.drawPersonBubble(
    center: Offset,
    radius: Float,
    colors: List<Color>,
    backdropColor: Color,
    isCenter: Boolean = false,
    isDark: Boolean = true,
) {
    drawCircle(color = backdropColor, radius = radius, center = center)

    when {
        colors.isEmpty() -> {
            drawCircle(
                color = if (isDark) {
                    Color.White.copy(alpha = if (isCenter) 0.22f else 0.14f)
                } else {
                    Color.White.copy(alpha = if (isCenter) 0.38f else 0.26f)
                },
                radius = radius,
                center = center,
            )
            drawCircle(
                color = if (isDark) {
                    Color.White.copy(alpha = if (isCenter) 0.55f else 0.35f)
                } else {
                    Color.White.copy(alpha = if (isCenter) 0.35f else 0.28f)
                },
                radius = radius,
                center = center,
                style = Stroke(width = if (isCenter) 2.5f else 1.5f),
            )
        }
        colors.size == 1 -> {
            drawCircle(color = colors.first().copy(alpha = 0.92f), radius = radius, center = center)
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
            color = color.copy(alpha = 0.92f),
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
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
    labelColors: LivingTreeTextContrast.LabelColors,
    fontSizeSp: Float,
    showPlate: Boolean,
    onBubble: Boolean = false,
) {
    val style = TextStyle(
        color = labelColors.text,
        fontSize = fontSizeSp.sp,
        textAlign = TextAlign.Center,
    )
    val layout = textMeasurer.measure(text, style)
    val platePaddingH = if (onBubble) 10f else 8f
    val platePaddingV = if (onBubble) 5f else 4f
    val plateWidth = layout.size.width + platePaddingH * 2f
    val plateHeight = layout.size.height + platePaddingV * 2f
    val plateTopLeft = Offset(
        x = center.x - plateWidth / 2f,
        y = center.y - plateHeight / 2f,
    )

    if (showPlate) {
        drawRoundRect(
            color = labelColors.plateFill,
            topLeft = plateTopLeft,
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(plateHeight / 2f, plateHeight / 2f),
        )
        drawRoundRect(
            color = labelColors.plateBorder,
            topLeft = plateTopLeft,
            size = Size(plateWidth, plateHeight),
            cornerRadius = CornerRadius(plateHeight / 2f, plateHeight / 2f),
            style = Stroke(width = 1f),
        )
    }

    val textTopLeft = Offset(
        x = center.x - layout.size.width / 2f,
        y = center.y - layout.size.height / 2f,
    )
    drawOutlinedText(
        textMeasurer = textMeasurer,
        text = text,
        style = style,
        topLeft = textTopLeft,
        outlineColor = labelColors.outline,
    )
}

private fun DrawScope.drawOutlinedText(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    topLeft: Offset,
    outlineColor: Color,
) {
    val outlineStyle = style.copy(color = outlineColor)
    val offsets = listOf(
        Offset(-1f, 0f),
        Offset(1f, 0f),
        Offset(0f, -1f),
        Offset(0f, 1f),
    )
    offsets.forEach { offset ->
        val outlineLayout = textMeasurer.measure(text, outlineStyle)
        drawText(outlineLayout, topLeft = topLeft + offset)
    }
    val layout = textMeasurer.measure(text, style)
    drawText(layout, topLeft = topLeft)
}
