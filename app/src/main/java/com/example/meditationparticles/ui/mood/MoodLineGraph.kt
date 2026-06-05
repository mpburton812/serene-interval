package com.example.meditationparticles.ui.mood

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.ui.components.moodColor

@Composable
fun MoodLineGraph(
    entries: List<MoodEntryEntity>,
    startMillis: Long,
    endMillis: Long,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.recordedAtMillis }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val lineColor = MaterialTheme.colorScheme.primary
    val rangeMillis = (endMillis - startMillis).coerceAtLeast(1L)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val width = size.width
        val height = size.height
        val padding = 16f
        val graphWidth = width - padding * 2
        val graphHeight = height - padding * 2

        for (level in MoodScale.MIN..MoodScale.MAX) {
            val y = padding + graphHeight * (1f - (level - MoodScale.MIN) / (MoodScale.MAX - MoodScale.MIN).toFloat())
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f,
            )
        }

        if (sorted.isEmpty()) return@Canvas

        val points = sorted.map { entry ->
            val xFraction = ((entry.recordedAtMillis - startMillis).toFloat() / rangeMillis).coerceIn(0f, 1f)
            val yFraction = (entry.moodLevel - MoodScale.MIN).toFloat() / (MoodScale.MAX - MoodScale.MIN).toFloat()
            Offset(
                x = padding + graphWidth * xFraction,
                y = padding + graphHeight * (1f - yFraction),
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
