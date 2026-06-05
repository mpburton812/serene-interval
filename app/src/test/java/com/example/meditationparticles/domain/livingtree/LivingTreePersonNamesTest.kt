package com.example.meditationparticles.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Test

class LivingTreePersonNamesTest {
    @Test
    fun parse_splitsOnNewlinesAndTrims() {
        val names = LivingTreePersonNames.parse(
            """
            Alex
            
            Jordan
              Sam  
            """.trimIndent(),
        )

        assertEquals(listOf("Alex", "Jordan", "Sam"), names)
    }
}
