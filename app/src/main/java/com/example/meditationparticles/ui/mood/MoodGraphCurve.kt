package com.example.meditationparticles.ui.mood

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

object MoodGraphCurve {
    fun buildSmoothPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path
        if (points.size == 1) {
            path.moveTo(points.first().x, points.first().y)
            return path
        }

        path.moveTo(points.first().x, points.first().y)
        for (index in 0 until points.lastIndex) {
            val p0 = points.getOrElse(index - 1) { points.first() }
            val p1 = points[index]
            val p2 = points[index + 1]
            val p3 = points.getOrElse(index + 2) { points.last() }

            val control1 = Offset(
                x = p1.x + (p2.x - p0.x) / 6f,
                y = p1.y + (p2.y - p0.y) / 6f,
            )
            val control2 = Offset(
                x = p2.x - (p3.x - p1.x) / 6f,
                y = p2.y - (p3.y - p1.y) / 6f,
            )
            path.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
        }
        return path
    }
}
