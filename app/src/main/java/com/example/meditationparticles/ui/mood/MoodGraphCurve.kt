package com.example.meditationparticles.ui.mood

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

data class MoodGraphStepSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

object MoodGraphCurve {
    /**
     * Step path: each point is approached horizontally from the left and left horizontally
     * to the right (post-step / step-after interpolation).
     */
    fun buildStepPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points.first().x, points.first().y)
        stepSegments(points).forEach { segment ->
            path.lineTo(segment.x2, segment.y2)
        }
        return path
    }

    fun stepSegments(points: List<Offset>): List<MoodGraphStepSegment> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<MoodGraphStepSegment>()
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            segments += MoodGraphStepSegment(previous.x, previous.y, current.x, previous.y)
            segments += MoodGraphStepSegment(current.x, previous.y, current.x, current.y)
        }
        return segments
    }
}
