package com.safehaven.affirmations.domain.thermometer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermometerRulesTest {
    @Test
    fun canCreate_allowsFiveAndBlocksTheSixth() {
        assertTrue(ThermometerRules.canCreate(0))
        assertTrue(ThermometerRules.canCreate(4))
        assertFalse(ThermometerRules.canCreate(5))
    }

    @Test
    fun clampScore_staysBetweenZeroAndOneHundred() {
        assertEquals(0, ThermometerRules.clampScore(-4))
        assertEquals(42, ThermometerRules.clampScore(42))
        assertEquals(100, ThermometerRules.clampScore(140))
    }
}
