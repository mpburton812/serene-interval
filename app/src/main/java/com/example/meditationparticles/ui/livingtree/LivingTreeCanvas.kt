package com.example.meditationparticles.ui.livingtree

import androidx.compose.foundation.Canvas
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
import com.example.meditationparticles.ui.theme.isDarkScheme
import kotlin.math.hypot
import kotlin.math.roundToInt

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
    filterActive: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme(scheme)
    val glassOverlay = painterResource(R.drawable.breath_glass_sphere)
    val visualScale = (centerRadius / 80f).coerceIn(0.75f, 1.25f)
    val currentNodes by rememberUpdatedState(nodes)
    val currentOnPersonTap by rememberUpdatedState(onPersonTap)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(nodes) {
                detectTapGestures { offset ->
                    currentNodes.firstOrNull { node ->
                        hypot(
                            (offset.x - node.x).toDouble(),
                            (offset.y - node.y).toDouble(),
                        ) <= node.radius
                    }?.let { currentOnPersonTap(it.id) }
                }
            },
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

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
            maxFontSp = 15f,
        )
    }
}

private fun DrawScope.drawInSphereName(
    textMeasurer: TextMeasurer,
    name: String,
    center: Offset,
    radius: Float,
    maxFontSp: Float = 13f,
) {
    val textColor = Color.Black
    val plateFill = Color.White
    val plateBorder = Color.Black.copy(alpha = 0.18f)
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
    val plateTopLeft = Offset(
        x = topLeft.x - platePaddingH,
        y = topLeft.y - platePaddingV,
    )
    val plateSize = Size(
        width = layout.size.width + platePaddingH * 2f,
        height = layout.size.height + platePaddingV * 2f,
    )
    val plateCorner = minOf(plateSize.height / 2f, plateSize.width / 2f, 14f).coerceAtLeast(6f)
    val cornerRadius = CornerRadius(plateCorner, plateCorner)
    drawRoundRect(
        color = plateFill,
        topLeft = plateTopLeft,
        size = plateSize,
        cornerRadius = cornerRadius,
    )
    drawRoundRect(
        color = plateBorder,
        topLeft = plateTopLeft,
        size = plateSize,
        cornerRadius = cornerRadius,
        style = Stroke(width = 1f),
    )
    drawText(layout, topLeft = topLeft)
}
