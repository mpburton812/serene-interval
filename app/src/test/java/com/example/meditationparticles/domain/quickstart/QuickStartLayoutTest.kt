package com.example.meditationparticles.domain.quickstart

import com.example.meditationparticles.domain.settings.ExperienceSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickStartLayoutTest {
    private val allEnabled = ExperienceSettings(
        enableBreathing = true,
        enableTimer = true,
        enableAffirmations = true,
        enableToolkit = true,
        enableVisuals = true,
    )

    @Test
    fun defaultSelection_returnsFirstFourEnabledTools() {
        val selection = QuickStartLayout.defaultSelection(allEnabled)
        assertEquals(
            listOf(
                QuickStartId.BREATHING,
                QuickStartId.TIMER,
                QuickStartId.AFFIRMATIONS,
                QuickStartId.TOOLKIT,
            ),
            selection,
        )
    }

    @Test
    fun toggleSelection_capsAtFour() {
        val initial = QuickStartLayout.defaultSelection(allEnabled)
        val withFifth = QuickStartLayout.toggleSelection(initial, QuickStartId.VISUALS, allEnabled)
        assertEquals(4, withFifth.size)
        assertFalse(QuickStartId.VISUALS in withFifth)
    }

    @Test
    fun hasValidSelection_requiresFourEnabledChoices() {
        val partial = listOf(QuickStartId.BREATHING, QuickStartId.TIMER)
        assertFalse(QuickStartLayout.hasValidSelection(partial, allEnabled))
        assertTrue(QuickStartLayout.hasValidSelection(QuickStartLayout.defaultSelection(allEnabled), allEnabled))
    }

    @Test
    fun normalizeSelection_dropsDisabledTools() {
        val settings = allEnabled.copy(enableToolkit = false)
        val normalized = QuickStartLayout.normalizeSelection(
            listOf(
                QuickStartId.BREATHING,
                QuickStartId.TIMER,
                QuickStartId.AFFIRMATIONS,
                QuickStartId.TOOLKIT,
            ),
            settings,
        )
        assertEquals(4, normalized.size)
        assertFalse(QuickStartId.TOOLKIT in normalized)
    }
}
