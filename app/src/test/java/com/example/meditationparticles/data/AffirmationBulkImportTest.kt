package com.example.meditationparticles.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AffirmationBulkImportTest {
    @Test
    fun parseAffirmationLines_splitsOnNewlinesAndTrims() {
        val raw = """
            I am calm
              I am present

            I am enough
        """.trimIndent()

        assertEquals(
            listOf("I am calm", "I am present", "I am enough"),
            parseAffirmationLines(raw),
        )
    }

    @Test
    fun parseAffirmationLines_ignoresBlankLines() {
        assertEquals(listOf<String>(), parseAffirmationLines("\n\n  \n"))
    }

    @Test
    fun parseAffirmationLines_handlesSingleLine() {
        assertEquals(listOf("One line"), parseAffirmationLines("One line"))
    }
}
