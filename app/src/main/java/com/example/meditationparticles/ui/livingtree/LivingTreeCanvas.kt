package com.example.meditationparticles.ui.livingtree

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.meditationparticles.R
import com.example.meditationparticles.canvas.LivingTreeSphereFill
import com.example.meditationparticles.canvas.drawLivingTreeGlassSphere
import com.example.meditationparticles.canvas.drawLivingTreeSpoke
import com.example.meditationparticles.domain.livingtree.LivingTreeLayout
import com.example.meditationparticles.domain.livingtree.LivingTreeTextContrast
import com.example.meditationparticles.ui.theme.isDarkScheme
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
    onPersonDragPosition: ((Long, LivingTreeLayout.StoredPosition) -> Unit)? = null,
    onPersonDragEnd: ((Long, LivingTreeLayout.StoredPosition) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    val view = LocalView.current
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme(scheme)
    val glassOverlay = painterResource(R.drawable.breath_glass_sphere)
    val pipeTexture = painterResource(R.drawable.breath_pipe_texture)
    val visualScale = (centerRadius / 80f).coerceIn(0.75f, 1.25f)
    val currentNodes by rememberUpdatedState(nodes)
    val currentOnPersonTap by rememberUpdatedState(onPersonTap)
    val currentOnPersonDragPosition by rememberUpdatedState(onPersonDragPosition)
    val currentOnPersonDragEnd by rememberUpdatedState(onPersonDragEnd)

    val shimmerTransition = rememberInfiniteTransition(label = "pipe_shimmer")
    val shimmerPhase by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_phase",
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(orbitRadius) {
                coroutineScope {
                    var suppressTap = false

                    launch {
                        detectTapGestures { offset ->
                            if (suppressTap) return@detectTapGestures
                            currentNodes.firstOrNull { node ->
                                hypot(
                                    (offset.x - node.x).toDouble(),
                                    (offset.y - node.y).toDouble(),
                                ) <= node.radius
                            }?.let { currentOnPersonTap(it.id) }
                        }
                    }

                    if (currentOnPersonDragPosition != null && currentOnPersonDragEnd != null && orbitRadius > 0f) {
                        launch {
                            var draggingId: Long? = null
                            var dragMoved = false
                            var grabOffset = Offset.Zero
                            var lastPosition = LivingTreeLayout.StoredPosition(angleRadians = 0.0)

                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    val node = currentNodes.firstOrNull { candidate ->
                                        hypot(
                                            (offset.x - candidate.x).toDouble(),
                                            (offset.y - candidate.y).toDouble(),
                                        ) <= candidate.radius
                                    }
                                    draggingId = node?.id
                                    dragMoved = false
                                    suppressTap = false
                                    if (node != null) {
                                        grabOffset = offset - Offset(node.x, node.y)
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                },
                                onDrag = { change, _ ->
                                    val id = draggingId ?: return@detectDragGesturesAfterLongPress
                                    dragMoved = true
                                    suppressTap = true
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val targetCenter = change.position - grabOffset
                                    lastPosition = LivingTreeLayout.positionFromCanvasPoint(
                                        centerX = centerX,
                                        centerY = centerY,
                                        orbitRadius = orbitRadius,
                                        x = targetCenter.x,
                                        y = targetCenter.y,
                                    )
                                    currentOnPersonDragPosition?.invoke(id, lastPosition)
                                    change.consume()
                                },
                                onDragEnd = {
                                    draggingId?.let { id ->
                                        if (dragMoved) {
                                            currentOnPersonDragEnd?.invoke(id, lastPosition)
                                        }
                                    }
                                    draggingId = null
                                    suppressTap = false
                                },
                                onDragCancel = {
                                    draggingId = null
                                    suppressTap = false
                                },
                            )
                        }
                    }
                }
            },
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        nodes.forEach { node ->
            drawLivingTreeSpoke(
                center = Offset(centerX, centerY),
                centerRadius = centerRadius,
                nodeCenter = Offset(node.x, node.y),
                nodeRadius = node.radius,
                scale = visualScale,
                pipeTexture = pipeTexture,
                shimmerPhase = shimmerPhase,
            )
        }

        nodes.forEach { node ->
            val colors = if (filterActive && node.isFilteredMatch) node.bubbleColors else emptyList()
            drawLivingTreeGlassSphere(
                center = Offset(node.x, node.y),
                radius = node.radius,
                fill = LivingTreeSphereFill(colors = colors),
                scale = visualScale,
                glassOverlay = glassOverlay,
                isDark = isDark,
            )
            drawInSphereName(
                textMeasurer = textMeasurer,
                name = node.name,
                center = Offset(node.x, node.y),
                radius = node.radius,
                bubbleColors = colors,
                scheme = scheme,
                isDark = isDark,
            )
        }

        drawLivingTreeGlassSphere(
            center = Offset(centerX, centerY),
            radius = centerRadius,
            fill = LivingTreeSphereFill(colors = emptyList(), isCenter = true),
            scale = visualScale,
            glassOverlay = glassOverlay,
            isDark = isDark,
            highlightAlpha = 0.62f,
        )
        drawInSphereName(
            textMeasurer = textMeasurer,
            name = centerLabel,
            center = Offset(centerX, centerY),
            radius = centerRadius,
            bubbleColors = emptyList(),
            scheme = scheme,
            isDark = isDark,
            maxFontSp = 15f,
        )
    }
}

private fun DrawScope.drawInSphereName(
    textMeasurer: TextMeasurer,
    name: String,
    center: Offset,
    radius: Float,
    bubbleColors: List<Color>,
    scheme: androidx.compose.material3.ColorScheme,
    isDark: Boolean,
    maxFontSp: Float = 13f,
) {
    val labelColors = LivingTreeTextContrast.labelColors(scheme, isDark)
    val textColor = LivingTreeTextContrast.textOnBubbleFill(bubbleColors, scheme, isDark)
    val maxWidth = radius * 1.55f
    val maxHeight = radius * 1.1f
    val minFontSp = 8f
    var fontSize = maxFontSp
    var layout = textMeasurer.measure(
        text = name,
        style = TextStyle(
            color = textColor,
            fontSize = fontSize.sp,
            textAlign = TextAlign.Center,
        ),
        constraints = Constraints(
            maxWidth = maxWidth.roundToInt().coerceAtLeast(1),
            maxHeight = maxHeight.roundToInt().coerceAtLeast(1),
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )

    while (
        fontSize > minFontSp &&
        (layout.size.width > maxWidth || layout.size.height > maxHeight)
    ) {
        fontSize -= 1f
        layout = textMeasurer.measure(
            text = name,
            style = TextStyle(
                color = textColor,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center,
            ),
            constraints = Constraints(
                maxWidth = maxWidth.roundToInt().coerceAtLeast(1),
                maxHeight = maxHeight.roundToInt().coerceAtLeast(1),
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }

    val topLeft = Offset(
        x = center.x - layout.size.width / 2f,
        y = center.y - layout.size.height / 2f,
    )
    val platePaddingH = 6f
    val platePaddingV = 3f
    val cornerRadius = CornerRadius(6f, 6f)
    val plateTopLeft = Offset(
        x = topLeft.x - platePaddingH,
        y = topLeft.y - platePaddingV,
    )
    val plateSize = Size(
        width = layout.size.width + platePaddingH * 2f,
        height = layout.size.height + platePaddingV * 2f,
    )
    drawRoundRect(
        color = labelColors.plateFill,
        topLeft = plateTopLeft,
        size = plateSize,
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        color = labelColors.plateBorder,
        topLeft = plateTopLeft,
        size = plateSize,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1f),
    )
    drawText(layout, topLeft = topLeft)
}
