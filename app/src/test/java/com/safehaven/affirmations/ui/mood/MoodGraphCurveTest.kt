package com.safehaven.affirmations.ui.mood

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodGraphCurveTest {
    @Test
    fun stepSegments_usesHorizontalThenVerticalSegments() {
        val points = listOf(
            Offset(0f, 100f),
            Offset(50f, 50f),
            Offset(100f, 80f),
        )

        val segments = MoodGraphCurve.stepSegments(points)

        assertEquals(
            listOf(
                MoodGraphStepSegment(0f, 100f, 50f, 100f),
                MoodGraphStepSegment(50f, 100f, 50f, 50f),
                MoodGraphStepSegment(50f, 50f, 100f, 50f),
                MoodGraphStepSegment(100f, 50f, 100f, 80f),
            ),
            segments,
        )
    }

    @Test
    fun smoothCubicSegments_easesBetweenThreePoints() {
        val points = listOf(
            Offset(0f, 10f),
            Offset(40f, 50f),
            Offset(80f, 20f),
        )
        val segments = MoodGraphCurve.smoothCubicSegments(points)
        assertEquals(2, segments.size)
        assertEquals(40f, segments[0].endX, 0.001f)
        assertEquals(50f, segments[0].endY, 0.001f)
        assertEquals(80f, segments[1].endX, 0.001f)
        assertEquals(20f, segments[1].endY, 0.001f)
        assertTrue(segments[0].c1x >= points[0].x)
        assertTrue(segments[0].c1x <= points[1].x)
    }

    @Test
    fun smoothCubicSegments_neverMovesBackwardInX() {
        val points = listOf(
            Offset(10f, 80f),
            Offset(40f, 20f),
            Offset(45f, 90f),
            Offset(90f, 30f),
            Offset(95f, 10f),
        )
        val segments = MoodGraphCurve.smoothCubicSegments(points)
        var startX = points.first().x
        var startY = points.first().y
        segments.forEach { segment ->
            var previousX = startX
            for (step in 1..20) {
                val t = step / 20f
                val x = MoodGraphCurve.sampleCubicX(startX, segment, t)
                assertTrue(
                    "X must be non-decreasing (prev=$previousX next=$x)",
                    x + 0.05f >= previousX,
                )
                previousX = x
            }
            val minY = minOf(startY, segment.endY)
            val maxY = maxOf(startY, segment.endY)
            for (step in 0..20) {
                val t = step / 20f
                val y = MoodGraphCurve.sampleCubicY(startY, segment, t)
                assertTrue(
                    "Y must stay within endpoint range ($minY..$maxY), got $y",
                    y in (minY - 0.05f)..(maxY + 0.05f),
                )
            }
            startX = segment.endX
            startY = segment.endY
        }
    }

    @Test
    fun coalesceByX_mergesSamePixelPoints() {
        val coalesced = MoodGraphCurve.coalesceByX(
            listOf(
                Offset(10f, 10f),
                Offset(10.2f, 30f),
                Offset(50f, 40f),
            ),
        )
        assertEquals(2, coalesced.size)
        assertEquals(50f, coalesced[1].x, 0.001f)
    }

    @Test
    fun stepSegments_singlePoint_returnsEmptyList() {
        assertEquals(emptyList<MoodGraphStepSegment>(), MoodGraphCurve.stepSegments(listOf(Offset(1f, 2f))))
    }
}
