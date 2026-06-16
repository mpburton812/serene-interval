package com.example.meditationparticles.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.mood.MoodGraphAxisFormatter
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodGraphSeriesBuilder
import com.example.meditationparticles.domain.mood.MoodMonthGraphMode
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
    graphEndMillis: Long,
    average: Double?,
    monthGraphMode: MoodMonthGraphMode = MoodMonthGraphMode.TOTAL_AVERAGE,
    modifier: Modifier = Modifier,
) {
    val visibleEntries = remember(entries, graphEndMillis) {
        MoodGraphSeriesBuilder.entriesWithinGraph(entries, graphEndMillis)
    }
    val sorted = remember(visibleEntries) { visibleEntries.sortedBy { it.recordedAtMillis } }
    val zoneId = remember { ZoneId.systemDefault() }
    val locale = remember { Locale.getDefault() }
    val axisColor = MaterialTheme.colorScheme.outline
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val averageLineColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
    val useMonthRolling = period == MoodGraphPeriod.MONTH &&
        monthGraphMode == MoodMonthGraphMode.ROLLING_7_DAY
    val graphPoints = remember(sorted, period, monthGraphMode, startMillis, endMillis, graphEndMillis, zoneId) {
        if (useMonthRolling) {
            MoodGraphSeriesBuilder.monthRollingAverageSeries(
                entries = sorted,
                startMillis = startMillis,
                endMillis = endMillis,
                graphEndMillis = graphEndMillis,
                zoneId = zoneId,
            )
        } else {
            MoodGraphSeriesBuilder.entryPoints(sorted)
        }
    }
    val xDomain = remember(graphPoints, useMonthRolling, startMillis, graphEndMillis) {
        if (useMonthRolling) {
            MoodGraphSeriesBuilder.monthDomainMillis(startMillis, graphEndMillis)
        } else {
            MoodGraphSeriesBuilder.dataSpanMillis(graphPoints)
                ?: (startMillis to (graphEndMillis - 1).coerceAtLeast(startMillis))
        }
    }
    val showAverageLine = average != null && period != MoodGraphPeriod.CALENDAR
    val xTicks = remember(period, startMillis, graphEndMillis, zoneId, locale) {
        MoodGraphAxisFormatter.xAxisTicks(period, startMillis, graphEndMillis, zoneId, locale)
    }
    val yTicks = remember { MoodGraphAxisFormatter.yAxisTicks() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        val width = size.width
        val height = size.height
        val leftPadding = 44f
        val rightPadding = 8f
        val topPadding = 8f
        val bottomPadding = 28f
        val plotLeft = leftPadding
        val plotRight = width - rightPadding
        val plotTop = topPadding
        val plotBottom = height - bottomPadding
        val graphWidth = plotRight - plotLeft
        val graphHeight = plotBottom - plotTop
        val (domainStart, domainEnd) = xDomain

        fun xForFraction(fraction: Float): Float = plotLeft + graphWidth * fraction.coerceIn(0f, 1f)

        fun xForPoint(xMillis: Long): Float {
            val fraction = if (useMonthRolling) {
                MoodGraphSeriesBuilder.monthXFraction(xMillis, startMillis, graphEndMillis, zoneId)
            } else {
                MoodGraphSeriesBuilder.xFraction(
                    xMillis = xMillis,
                    domainStartMillis = domainStart,
                    domainEndMillis = domainEnd,
                    singlePointCentered = graphPoints.size == 1,
                )
            }
            return xForFraction(fraction)
        }

        fun yForLevel(level: Double): Float {
            val yFraction = (level - MoodScale.MIN).toFloat() / (MoodScale.MAX - MoodScale.MIN).toFloat()
            return plotTop + graphHeight * (1f - yFraction.coerceIn(0f, 1f))
        }

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
            drawContext.canvas.nativeCanvas.drawText(
                tick.label,
                plotLeft - 8f,
                yLine + labelPaint.textSize / 3f,
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

        if (showAverageLine) {
            val averageY = yForLevel(average)
            drawLine(
                color = averageLineColor,
                start = Offset(plotLeft, averageY),
                end = Offset(plotRight, averageY),
                strokeWidth = 4.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)),
            )
        }

        if (graphPoints.isEmpty()) return@Canvas

        val points = graphPoints.map { point ->
            Offset(
                x = xForPoint(point.xMillis),
                y = yForLevel(point.yLevel),
            )
        }

        val dotRadius = if (useMonthRolling) 10f else 8f

        if (points.size == 1) {
            val level = graphPoints.first().yLevel
            val moodLevel = if (useMonthRolling) {
                MoodScale.averageToLevel(level) ?: MoodScale.MIN
            } else {
                sorted.first().moodLevel
            }
            drawCircle(
                color = moodColor(moodLevel),
                radius = dotRadius,
                center = points.first(),
            )
            return@Canvas
        }

        val path = MoodGraphCurve.buildStepPath(points)
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = if (useMonthRolling) 4f else 5f, cap = StrokeCap.Round),
        )
        points.forEachIndexed { index, point ->
            val level = graphPoints[index].yLevel
            val moodLevel = if (useMonthRolling) {
                MoodScale.averageToLevel(level) ?: MoodScale.MIN
            } else {
                sorted[index].moodLevel
            }
            drawCircle(
                color = moodColor(moodLevel),
                radius = dotRadius,
                center = point,
            )
        }
    }
}
