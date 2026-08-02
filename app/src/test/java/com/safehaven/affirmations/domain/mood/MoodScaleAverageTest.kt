package com.safehaven.affirmations.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodScaleAverageTest {
    @Test
    fun averageToLevel_roundsToNearestMoodLevel() {
        assertEquals(2, MoodScale.averageToLevel(2.4))
        assertEquals(3, MoodScale.averageToLevel(2.6))
        assertEquals(4, MoodScale.averageToLevel(4.8))
    }

    @Test
    fun averageToLevel_returnsNullForMissingAverage() {
        assertNull(MoodScale.averageToLevel(null))
    }
}
