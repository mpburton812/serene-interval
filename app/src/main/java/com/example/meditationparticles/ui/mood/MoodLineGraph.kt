package com.example.meditationparticles.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.mood.MoodGraphAxisFormatter
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.ui.components.moodColor
import java.time.ZoneId
import java.util.Locale

@Composable
fun MoodLineGraph(
    entries: List<MoodEntryEntity>,
    period: MoodGraphPeriod,
    startMillis: Long,
    endMillis: Long,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.recordedAtMillis }
    val zoneId = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val rangeMillis = (endMillis - startMillis).coerceAtLeast(1L)
    val xTicks = remember(period, startMillis, endMillis, zoneId, locale) {
        MoodGraphAxisFormatter.xAxisTicks(period, startMillis, endMillis, zoneId, locale)
    }
    val yTicks = remember { MoodGraphAxisFormatter.yAxisTicks() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val width = size.width
        val height = size.height
        val leftPadding = 44f
        val rightPadding = 12f
        val topPadding = 8f
        val bottomPadding = 28f
        val plotLeft = leftPadding
        val plotRight = width - rightPadding
        val plotTop = topPadding
        val plotBottom = height - bottomPadding
        val graphWidth = plotRight - plotLeft
        val graphHeight = plotBottom - plotTop

        fun xForFraction(fraction: Float): Float = plotLeft + graphWidth * fraction.coerceIn(0f, 1f)

        val labelPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }
        val yLabelPaint = android.graphics.Paint(labelPaint).apply {
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val xLabelPaint = android.graphics.Paint(labelPaint).apply {
            textAlign = android.graphics.Paint.Align.CENTER
        }

        drawLine(
            color = axisColor,
            start = Offset(plotLeft, plotTop),
            end = Offset(plotLeft, plotBottom),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = axisColor,
            start = Offset(plotLeft, plotBottom),
            end = Offset(plotRight, plotBottom),
            strokeWidth = 1.5f,
        )

        yTicks.forEach { tick ->
            val yLine = plotTop + graphHeight * (1f - tick.positionFraction)

            drawLine(
                color = gridColor,
                start = Offset(plotLeft, yLine),
                end = Offset(plotRight, yLine),
                strokeWidth = 1f,
            )
            drawLine(
                color = axisColor,
                start = Offset(plotLeft - 4f, yLine),
                end = Offset(plotLeft, yLine),
                strokeWidth = 1.5f,
            )

            val textY = yLine + labelPaint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(
                tick.label,
                plotLeft - 8f,
                textY,
                yLabelPaint,
            )
        }

        xTicks.forEach { tick ->
            val x = xForFraction(tick.positionFraction)
            drawLine(
                color = axisColor,
                start = Offset(x, plotBottom),
                end = Offset(x, plotBottom + 4f),
                strokeWidth = 1.5f,
            )
            drawContext.canvas.nativeCanvas.drawText(
                tick.label,
                x,
                plotBottom + labelPaint.textSize + 4f,
                xLabelPaint,
            )
        }

        if (sorted.isEmpty()) return@Canvas

        val points = sorted.map { entry ->
            val xFraction = ((entry.recordedAtMillis - startMillis).toFloat() / rangeMillis).coerceIn(0f, 1f)
            val yFraction = (entry.moodLevel - MoodScale.MIN).toFloat() / (MoodScale.MAX - MoodScale.MIN).toFloat()
            Offset(
                x = xForFraction(xFraction),
                y = plotTop + graphHeight * (1f - yFraction),
            )
        }

        if (points.size == 1) {
            val point = points.first()
            drawCircle(
                color = moodColor(sorted.first().moodLevel),
                radius = 6f,
                center = point,
            )
            return@Canvas
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        points.forEachIndexed { index, point ->
            drawCircle(
                color = moodColor(sorted[index].moodLevel),
                radius = 5f,
                center = point,
            )
        }
    }
}
