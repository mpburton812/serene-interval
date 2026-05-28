package com.example.meditationparticles.domain.quickstart

import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
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
    private val allToolkitTools = ToolkitLayout.defaultEnabledTools()

    @Test
    fun defaultSelection_returnsFourGranularShortcuts() {
        val selection = QuickStartLayout.defaultSelection(allEnabled, allToolkitTools)
        assertEquals(4, selection.size)
        assertEquals(QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id), selection[0])
        assertEquals(QuickStartTarget.Timer, selection[1])
        assertEquals(QuickStartTarget.Affirmations, selection[2])
        assertTrue(selection[3] is QuickStartTarget.Toolkit)
    }

    @Test
    fun toggleSelection_capsAtFour() {
        val initial = QuickStartLayout.defaultSelection(allEnabled, allToolkitTools)
        val withFifth = QuickStartLayout.toggleSelection(
            initial,
            QuickStartTarget.Visuals,
            allEnabled,
            allToolkitTools,
        )
        assertEquals(4, withFifth.size)
        assertFalse(QuickStartTarget.Visuals in withFifth)
    }

    @Test
    fun toggleSelection_deselectsSelectedTarget() {
        val initial = QuickStartLayout.defaultSelection(allEnabled, allToolkitTools)
        val toRemove = initial.first()
        val afterDeselect = QuickStartLayout.toggleSelection(
            initial,
            toRemove,
            allEnabled,
            allToolkitTools,
        )
        assertEquals(3, afterDeselect.size)
        assertFalse(toRemove in afterDeselect)
    }

    @Test
    fun sanitizeSelection_doesNotPadPartialSelection() {
        val partial = listOf(
            QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id),
            QuickStartTarget.Timer,
        )
        val sanitized = QuickStartLayout.sanitizeSelection(partial, allEnabled, allToolkitTools)
        assertEquals(2, sanitized.size)
        assertEquals(partial, sanitized)
    }

    @Test
    fun hasValidSelection_requiresFourEnabledChoices() {
        val partial = listOf(
            QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id),
            QuickStartTarget.Timer,
        )
        assertFalse(QuickStartLayout.hasValidSelection(partial, allEnabled, allToolkitTools))
        assertTrue(
            QuickStartLayout.hasValidSelection(
                QuickStartLayout.defaultSelection(allEnabled, allToolkitTools),
                allEnabled,
                allToolkitTools,
            ),
        )
    }

    @Test
    fun normalizeSelection_dropsDisabledToolkitTools() {
        val settings = allEnabled.copy(enableToolkit = false)
        val normalized = QuickStartLayout.normalizeSelection(
            listOf(
                QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id),
                QuickStartTarget.Timer,
                QuickStartTarget.Affirmations,
                QuickStartTarget.Toolkit(ToolkitToolId.ThoughtDump),
            ),
            settings,
            allToolkitTools,
        )
        assertEquals(4, normalized.size)
        assertFalse(normalized.any { it is QuickStartTarget.Toolkit })
    }

    @Test
    fun quickStartTarget_decode_supportsLegacyTabKeys() {
        assertEquals(
            QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id),
            QuickStartTarget.decode("BREATHING"),
        )
        assertEquals(
            QuickStartTarget.Toolkit(ToolkitToolId.ThoughtDump),
            QuickStartTarget.decode("TOOLKIT"),
        )
        assertEquals(
            QuickStartTarget.Breathing(BreathingPattern.Resonant.id),
            QuickStartTarget.decode("breathing:resonant"),
        )
    }
}
