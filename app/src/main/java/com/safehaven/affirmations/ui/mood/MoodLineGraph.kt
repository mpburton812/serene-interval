package com.safehaven.affirmations.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safehaven.affirmations.data.local.MoodEntryEntity
import com.safehaven.affirmations.domain.mood.MoodGraphAxisFormatter
import com.safehaven.affirmations.domain.mood.MoodGraphPeriod
import com.safehaven.affirmations.domain.mood.MoodGraphSeriesBuilder
import com.safehaven.affirmations.domain.mood.MoodMonthGraphMode
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.ui.components.moodColor
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
    val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val averageLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.95f)
    val useMonthRolling = period == MoodGraphPeriod.MONTH &&
        monthGraphMode == MoodMonthGraphMode.ROLLING_7_DAY
    val useMonthDailyAverage = period == MoodGraphPeriod.MONTH &&
        monthGraphMode == MoodMonthGraphMode.TOTAL_AVERAGE
    val graphPoints = remember(sorted, period, monthGraphMode, startMillis, endMillis, graphEndMillis, zoneId) {
        when {
            useMonthRolling -> MoodGraphSeriesBuilder.monthRollingAverageSeries(
                entries = sorted,
                startMillis = startMillis,
                endMillis = endMillis,
                graphEndMillis = graphEndMillis,
                zoneId = zoneId,
            )
            useMonthDailyAverage -> MoodGraphSeriesBuilder.dailyAverageSeries(sorted, zoneId)
            else -> MoodGraphSeriesBuilder.entryPoints(sorted)
        }
    }
    // Full calendar period for axis layout; graphEndMillis only filters which points appear.
    val xDomain = remember(startMillis, endMillis) {
        MoodGraphSeriesBuilder.periodDomainMillis(startMillis, endMillis)
    }
    val showAverageLine = average != null && period != MoodGraphPeriod.CALENDAR
    val xTicks = remember(period, startMillis, endMillis, zoneId, locale) {
        MoodGraphAxisFormatter.xAxisTicks(period, startMillis, endMillis, zoneId, locale)
    }
    val yTicks = remember { MoodGraphAxisFormatter.yAxisTicks() }
    val fillColors = remember {
        listOf(
            moodColor(4).copy(alpha = 0.55f),
            moodColor(3).copy(alpha = 0.45f),
            moodColor(2).copy(alpha = 0.40f),
            moodColor(1).copy(alpha = 0.50f),
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
    ) {
        val width = size.width
        val height = size.height
        val leftPadding = 52f
        val rightPadding = 8f
        val topPadding = 12f
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
            val fraction = if (period == MoodGraphPeriod.MONTH) {
                MoodGraphSeriesBuilder.monthXFraction(xMillis, startMillis, endMillis, zoneId)
            } else {
                MoodGraphSeriesBuilder.xFraction(
                    xMillis = xMillis,
                    domainStartMillis = domainStart,
                    domainEndMillis = domainEnd,
                    singlePointCentered = false,
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
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
            drawMoodFaceIcon(
                center = Offset(plotLeft - 22f, yLine),
                radius = 14f,
                level = tick.level,
            )
        }

        xTicks.forEach { tick ->
            val x = xForFraction(tick.positionFraction)
            drawLine(
                color = gridColor,
                start = Offset(x, plotTop),
                end = Offset(x, plotBottom),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
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
                strokeWidth = 3.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)),
            )
            drawContext.canvas.nativeCanvas.drawText(
                "Average",
                plotLeft + 6f,
                averageY - 6f,
                labelPaint,
            )
        }

        if (graphPoints.isEmpty()) return@Canvas

        val points = graphPoints.map { point ->
            Offset(
                x = xForPoint(point.xMillis),
                y = yForLevel(point.yLevel),
            )
        }

        val useDayAverageDots = useMonthRolling || useMonthDailyAverage
        val dotRadius = if (useMonthRolling) 9f else 7f

        if (points.size == 1) {
            val level = graphPoints.first().yLevel
            val moodLevel = if (useDayAverageDots) {
                MoodScale.averageToLevel(level) ?: MoodScale.MIN
            } else {
                sorted.first().moodLevel
            }
            drawCircle(color = Color.White, radius = dotRadius + 2f, center = points.first())
            drawCircle(color = moodColor(moodLevel), radius = dotRadius, center = points.first())
            return@Canvas
        }

        val linePath = MoodGraphCurve.buildSmoothPath(points)
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, plotBottom)
            lineTo(points.first().x, plotBottom)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = fillColors,
                startY = plotTop,
                endY = plotBottom,
            ),
        )
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = if (useMonthRolling) 3.5f else 4f, cap = StrokeCap.Round),
        )
        points.forEachIndexed { index, point ->
            val level = graphPoints[index].yLevel
            val moodLevel = if (useDayAverageDots) {
                MoodScale.averageToLevel(level) ?: MoodScale.MIN
            } else {
                sorted.getOrNull(index)?.moodLevel ?: MoodScale.averageToLevel(level) ?: MoodScale.MIN
            }
            drawCircle(color = Color.White, radius = dotRadius + 2.5f, center = point)
            drawCircle(color = moodColor(moodLevel), radius = dotRadius, center = point)
        }
    }
}

private fun DrawScope.drawMoodFaceIcon(center: Offset, radius: Float, level: Int) {
    val face = moodColor(level)
    drawCircle(color = face, radius = radius, center = center)
    val eyeY = center.y - radius * 0.18f
    val eyeR = radius * 0.12f
    drawCircle(Color.White.copy(alpha = 0.95f), eyeR, Offset(center.x - radius * 0.28f, eyeY))
    drawCircle(Color.White.copy(alpha = 0.95f), eyeR, Offset(center.x + radius * 0.28f, eyeY))
    val mouthPath = Path()
    val mouthY = center.y + radius * 0.22f
    val mouthW = radius * 0.45f
    when (MoodScale.migrateFromLegacy(level)) {
        1 -> {
            // Frown
            mouthPath.moveTo(center.x - mouthW, mouthY + radius * 0.12f)
            mouthPath.quadraticTo(center.x, mouthY - radius * 0.2f, center.x + mouthW, mouthY + radius * 0.12f)
        }
        2 -> {
            mouthPath.moveTo(center.x - mouthW, mouthY + radius * 0.05f)
            mouthPath.quadraticTo(center.x, mouthY - radius * 0.08f, center.x + mouthW, mouthY + radius * 0.05f)
        }
        3 -> {
            mouthPath.moveTo(center.x - mouthW, mouthY)
            mouthPath.lineTo(center.x + mouthW, mouthY)
        }
        else -> {
            // Smile
            mouthPath.moveTo(center.x - mouthW, mouthY - radius * 0.05f)
            mouthPath.quadraticTo(center.x, mouthY + radius * 0.35f, center.x + mouthW, mouthY - radius * 0.05f)
        }
    }
    drawPath(
        path = mouthPath,
        color = Color.White.copy(alpha = 0.95f),
        style = Stroke(width = radius * 0.14f, cap = StrokeCap.Round),
    )
}
