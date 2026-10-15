package com.safehaven.affirmations.domain.thermometer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class ThermometerChangeTextTest {
    @Test
    fun activityLineIncludesFromToReasonAndLowerPlan() {
        val line = ThermometerChangeText.activitySubtitle(
            name = "Stress",
            fromScore = 40,
            toScore = 62,
            reason = "argument with a coworker",
            lowerPlan = "take a walk",
        )
        assertEquals(
            "Stress - stress from 40 to 62, argument with a coworker. How to lower: take a walk",
            line,
        )
        assertEquals("Thermometer:", ThermometerChangeText.ACTIVITY_TITLE)
    }

    @Test
    fun firstReadingUsesBlankFromScore() {
        assertEquals(
            "Sleep - stress from — to 20",
            ThermometerChangeText.activitySubtitle("Sleep", null, 20, null, "  "),
        )
    }

    @Test
    fun previousScoreIsThePriorReadingForThatThermometer() {
        val previous = ThermometerChangeText.previousScores(
            listOf(
                reading(id = 1, thermometerId = 7, score = 40, recordedAt = 1),
                reading(id = 2, thermometerId = 8, score = 10, recordedAt = 2),
                reading(id = 3, thermometerId = 7, score = 62, recordedAt = 3),
            ),
        )
        assertEquals(mapOf(3L to 40), previous)
    }

    @Test
    fun closeSavesWhenScoreOrEitherNoteChanged() {
        assertFalse(
            ThermometerChangeText.shouldSaveOnClose(
                baselineScore = 40,
                score = 40,
                reason = "",
                lowerPlan = "",
                scoreTouched = false,
            ),
        )
        assertTrue(
            ThermometerChangeText.shouldSaveOnClose(
                baselineScore = 40,
                score = 55,
                reason = "",
                lowerPlan = "",
                scoreTouched = true,
            ),
        )
        assertTrue(
            ThermometerChangeText.shouldSaveOnClose(
                baselineScore = 40,
                score = 40,
                reason = "",
                lowerPlan = "walk",
                scoreTouched = false,
            ),
        )
        assertFalse(
            ThermometerChangeText.shouldSaveOnClose(
                baselineScore = null,
                score = 50,
                reason = "",
                lowerPlan = "",
                scoreTouched = false,
            ),
        )
    }

    @Test
    fun graphStampsAddTimeWhenReadingsShareADay() {
        TimeZone.getTimeZone("UTC").let { zone ->
            val day = 1_700_000_000_000L
            val labels = ThermometerGraphStamps.labels(
                recordedAt = listOf(day, day + 3_600_000L, day + 86_400_000L),
                locale = Locale.US,
                zone = zone,
            )
            assertEquals(listOf("11/14 10:13PM", "11/14 11:13PM", "11/15"), labels)
        }
    }

    private fun reading(id: Long, thermometerId: Long, score: Int, recordedAt: Long) =
        ThermometerReadingRef(id, thermometerId, score, recordedAt)
}
