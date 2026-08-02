package com.safehaven.affirmations.ui.mood

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

data class MoodGraphStepSegment(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
)

data class MoodGraphCubicSegment(
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float,
    val endX: Float,
    val endY: Float,
)

object MoodGraphCurve {
    /**
     * Catmull-Rom → cubic Bezier control points (JVM-safe; used by [buildSmoothPath]).
     */
    fun smoothCubicSegments(points: List<Offset>): List<MoodGraphCubicSegment> {
        if (points.size < 2) return emptyList()
        if (points.size == 2) {
            val a = points[0]
            val b = points[1]
            return listOf(
                MoodGraphCubicSegment(
                    c1x = a.x,
                    c1y = a.y,
                    c2x = b.x,
                    c2y = b.y,
                    endX = b.x,
                    endY = b.y,
                ),
            )
        }
        val segments = mutableListOf<MoodGraphCubicSegment>()
        for (i in 0 until points.size - 1) {
            val p0 = points[(i - 1).coerceAtLeast(0)]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
            segments += MoodGraphCubicSegment(
                c1x = p1.x + (p2.x - p0.x) / 6f,
                c1y = p1.y + (p2.y - p0.y) / 6f,
                c2x = p2.x - (p3.x - p1.x) / 6f,
                c2y = p2.y - (p3.y - p1.y) / 6f,
                endX = p2.x,
                endY = p2.y,
            )
        }
        return segments
    }

    /**
     * Smooth path through points using Catmull-Rom → cubic Bezier segments so the
     * line eases into each marker (matches the day/week/month graph redesign).
     */
    fun buildSmoothPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        path.moveTo(points.first().x, points.first().y)
        if (points.size == 1) return path
        smoothCubicSegments(points).forEach { segment ->
            path.cubicTo(
                segment.c1x,
                segment.c1y,
                segment.c2x,
                segment.c2y,
                segment.endX,
                segment.endY,
            )
        }
        return path
    }

    /** @deprecated Prefer [buildSmoothPath]; kept for tests of the old step geometry. */
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
