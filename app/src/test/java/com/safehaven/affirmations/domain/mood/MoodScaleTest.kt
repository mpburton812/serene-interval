package com.safehaven.affirmations.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoodScaleTest {
    @Test
    fun migrateFromLegacy_mapsOldFiveToFour() {
        assertEquals(4, MoodScale.migrateFromLegacy(5))
    }

    @Test
    fun migrateFromLegacy_preservesOneThroughFour() {
        assertEquals(1, MoodScale.migrateFromLegacy(1))
        assertEquals(2, MoodScale.migrateFromLegacy(2))
        assertEquals(3, MoodScale.migrateFromLegacy(3))
        assertEquals(4, MoodScale.migrateFromLegacy(4))
    }

    @Test
    fun migrateFromLegacy_clampsBelowMin() {
        assertEquals(1, MoodScale.migrateFromLegacy(0))
        assertEquals(1, MoodScale.migrateFromLegacy(-3))
    }

    @Test
    fun normalize_returnsNullForNullInput() {
        assertNull(MoodScale.normalize(null))
    }

    @Test
    fun normalize_migratesLegacyValues() {
        assertEquals(4, MoodScale.normalize(5))
        assertEquals(3, MoodScale.normalize(3))
    }

    @Test
    fun label_returnsColorNames() {
        assertEquals("Red", MoodScale.label(1))
        assertEquals("Yellow", MoodScale.label(2))
        assertEquals("Blue", MoodScale.label(3))
        assertEquals("Green", MoodScale.label(4))
        assertEquals("Green", MoodScale.label(5))
    }

    @Test
    fun colorArgb_usesDarkGreenForTopMood() {
        assertEquals(MoodScale.COLOR_GREEN, MoodScale.colorArgb(4))
        assertEquals(MoodScale.COLOR_GREEN, MoodScale.colorArgb(5))
    }
}
