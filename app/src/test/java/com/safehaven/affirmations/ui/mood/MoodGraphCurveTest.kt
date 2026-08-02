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
        // First control should pull toward the next point (not a hard corner).
        assertTrue(segments[0].c1x > points[0].x)
    }

    @Test
    fun stepSegments_singlePoint_returnsEmptyList() {
        assertEquals(emptyList<MoodGraphStepSegment>(), MoodGraphCurve.stepSegments(listOf(Offset(1f, 2f))))
    }
}
