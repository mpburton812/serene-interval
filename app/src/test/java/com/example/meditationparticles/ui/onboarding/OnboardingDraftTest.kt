package com.example.meditationparticles.ui.onboarding

import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDraftTest {
    @Test
    fun toggleToolkitTool_removingLastToolDisablesToolkitTab() {
        val draft = OnboardingDraft(
            enableToolkit = true,
            enabledToolkitTools = setOf(ToolkitToolId.ThoughtDump),
        )

        val updated = draft.toggleToolkitTool(ToolkitToolId.ThoughtDump)

        assertTrue(updated.enabledToolkitTools.isEmpty())
        assertFalse(updated.enableToolkit)
        assertFalse(updated.toolkitTabVisible)
    }

    @Test
    fun toggleToolkitTool_addingToolEnablesToolkitTab() {
        val draft = OnboardingDraft(
            enableToolkit = false,
            enabledToolkitTools = emptySet(),
        )

        val updated = draft.toggleToolkitTool(ToolkitToolId.AnxietyLog)

        assertTrue(updated.enableToolkit)
        assertTrue(updated.toolkitTabVisible)
    }

    @Test
    fun toExperienceSettings_hidesToolkitWhenNoToolsSelected() {
        val draft = OnboardingDraft(
            enableToolkit = true,
            enabledToolkitTools = emptySet(),
        )

        assertFalse(draft.toExperienceSettings().enableToolkit)
    }
}
