package com.example.meditationparticles.ui.mood

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
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
    fun stepSegments_singlePoint_returnsEmptyList() {
        assertEquals(emptyList<MoodGraphStepSegment>(), MoodGraphCurve.stepSegments(listOf(Offset(1f, 2f))))
    }
}
