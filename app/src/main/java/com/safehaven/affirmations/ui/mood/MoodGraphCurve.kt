package com.safehaven.affirmations.ui.mood

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
    /** Lower than classic Catmull-Rom 1/6 so curves ease gently into markers. */
    private const val TENSION = 1f / 10f
    private const val SAME_X_EPSILON = 0.75f

    /**
     * Catmull-Rom → cubic Bezier with clamped controls so each segment:
     * - never moves backward in X (time)
     * - never overshoots the Y range between its endpoints
     */
    fun smoothCubicSegments(points: List<Offset>): List<MoodGraphCubicSegment> {
        val pts = coalesceByX(points)
        if (pts.size < 2) return emptyList()
        if (pts.size == 2) {
            val a = pts[0]
            val b = pts[1]
            return listOf(linearSegment(a, b))
        }
        val segments = mutableListOf<MoodGraphCubicSegment>()
        for (i in 0 until pts.size - 1) {
            val p0 = pts[(i - 1).coerceAtLeast(0)]
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val p3 = pts[(i + 2).coerceAtMost(pts.lastIndex)]
            if (abs(p2.x - p1.x) <= SAME_X_EPSILON) {
                segments += linearSegment(p1, p2)
                continue
            }
            var c1x = p1.x + (p2.x - p0.x) * TENSION
            var c1y = p1.y + (p2.y - p0.y) * TENSION
            var c2x = p2.x - (p3.x - p1.x) * TENSION
            var c2y = p2.y - (p3.y - p1.y) * TENSION

            val minX = min(p1.x, p2.x)
            val maxX = max(p1.x, p2.x)
            val minY = min(p1.y, p2.y)
            val maxY = max(p1.y, p2.y)

            // Keep control X strictly within the segment so the cubic never folds left in time.
            c1x = c1x.coerceIn(minX, maxX)
            c2x = c2x.coerceIn(minX, maxX)
            if (p2.x >= p1.x) {
                c1x = c1x.coerceIn(p1.x, p2.x)
                c2x = c2x.coerceIn(p1.x, p2.x)
            } else {
                c1x = c1x.coerceIn(p2.x, p1.x)
                c2x = c2x.coerceIn(p2.x, p1.x)
            }

            // Keep control Y between the segment endpoints — no horns above/below markers.
            c1y = c1y.coerceIn(minY, maxY)
            c2y = c2y.coerceIn(minY, maxY)

            segments += MoodGraphCubicSegment(
                c1x = c1x,
                c1y = c1y,
                c2x = c2x,
                c2y = c2y,
                endX = p2.x,
                endY = p2.y,
            )
        }
        return segments
    }

    /**
     * Smooth path through points using clamped Catmull-Rom → cubic Bezier segments.
     */
    fun buildSmoothPath(points: List<Offset>): Path {
        val path = Path()
        val pts = coalesceByX(points)
        if (pts.isEmpty()) return path
        path.moveTo(pts.first().x, pts.first().y)
        if (pts.size == 1) return path
        smoothCubicSegments(pts).forEach { segment ->
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

    /** Merge near-identical X values (same day / same pixel) so cubics never reverse. */
    fun coalesceByX(points: List<Offset>, epsilon: Float = SAME_X_EPSILON): List<Offset> {
        if (points.size <= 1) return points
        val sorted = points.sortedBy { it.x }
        val result = mutableListOf<Offset>()
        var sumX = sorted[0].x
        var sumY = sorted[0].y
        var count = 1
        for (i in 1 until sorted.size) {
            val point = sorted[i]
            if (abs(point.x - sumX / count) <= epsilon) {
                sumX += point.x
                sumY += point.y
                count++
            } else {
                result += Offset(sumX / count, sumY / count)
                sumX = point.x
                sumY = point.y
                count = 1
            }
        }
        result += Offset(sumX / count, sumY / count)
        return result
    }

    /** Sample cubic X at t∈[0,1] for monotonicity tests. */
    fun sampleCubicX(startX: Float, segment: MoodGraphCubicSegment, t: Float): Float {
        val u = 1f - t
        return u * u * u * startX +
            3f * u * u * t * segment.c1x +
            3f * u * t * t * segment.c2x +
            t * t * t * segment.endX
    }

    fun sampleCubicY(startY: Float, segment: MoodGraphCubicSegment, t: Float): Float {
        val u = 1f - t
        return u * u * u * startY +
            3f * u * u * t * segment.c1y +
            3f * u * t * t * segment.c2y +
            t * t * t * segment.endY
    }

    private fun linearSegment(a: Offset, b: Offset): MoodGraphCubicSegment =
        MoodGraphCubicSegment(
            c1x = a.x,
            c1y = a.y,
            c2x = b.x,
            c2y = b.y,
            endX = b.x,
            endY = b.y,
        )

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
