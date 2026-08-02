package com.safehaven.affirmations.widget

import com.safehaven.affirmations.domain.mood.MoodScale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodWidgetLayoutTest {
    @Test
    fun moodLevels_coversFullScale() {
        val levels = MoodWidgetLayout.moodLevels()

        assertEquals(MoodScale.MAX - MoodScale.MIN + 1, levels.size)
        assertEquals(MoodScale.MIN, levels.first())
        assertEquals(MoodScale.MAX, levels.last())
    }

    @Test
    fun spacerCount_documentsEvenDistributionSlots() {
        val levels = MoodWidgetLayout.moodLevels()

        assertEquals(levels.size + 1, MoodWidgetLayout.spacerCount(levels.size))
    }
}
