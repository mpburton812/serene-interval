package com.example.meditationparticles.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Test

class LivingTreePersonNamesTest {
    @Test
    fun parse_splitsOnCommasAndTrims() {
        val names = LivingTreePersonNames.parse("Alex, Jordan,  Sam")

        assertEquals(listOf("Alex", "Jordan", "Sam"), names)
    }

    @Test
    fun parse_singleNameWithoutComma() {
        assertEquals(listOf("Alex"), LivingTreePersonNames.parse("Alex"))
    }

    @Test
    fun parse_ignoresBlankSegments() {
        val names = LivingTreePersonNames.parse("Alex,, Jordan, , Sam,")

        assertEquals(listOf("Alex", "Jordan", "Sam"), names)
    }
}
