package com.safehaven.affirmations.ui.onboarding

import com.safehaven.affirmations.domain.quickstart.QuickStartLayout
import com.safehaven.affirmations.domain.quickstart.QuickStartTarget
import com.safehaven.affirmations.domain.toolkit.ToolkitLayout
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingDraftToolkitToggleTest {

    @Test
    fun turningToolkitOffThenOn_succeedsAndKeepsValidQuickStart() {
        val start = OnboardingDraft(
            quickStartTargets = QuickStartLayout.defaultSelection(
                OnboardingDraft().toExperienceSettings().copy(onboardingCompleted = false),
            ),
        )
        assertTrue(start.enableToolkit)
        assertTrue(start.canComplete)

        val off = start.withToolEnabled(enableToolkit = false)
        assertFalse(off.enableToolkit)
        assertEquals(QuickStartLayout.SELECTION_COUNT, off.quickStartTargets.size)

        val on = off.withToolEnabled(enableToolkit = true)
        assertTrue(on.enableToolkit)
        assertTrue(on.enabledToolkitTools.isNotEmpty())
        assertTrue(on.toolkitTabVisible)
        assertEquals(QuickStartLayout.SELECTION_COUNT, on.quickStartTargets.size)
        assertTrue(on.canComplete)
    }

    @Test
    fun enableToolkitAgain_seedsDefaultsWhenToolsEmpty() {
        val empty = OnboardingDraft(
            enableToolkit = false,
            enabledToolkitTools = emptySet(),
            quickStartTargets = listOf(
                QuickStartTarget.Timer,
                QuickStartTarget.Affirmations,
            ),
        )

        val restored = empty.enableToolkitAgain()
        assertTrue(restored.enableToolkit)
        assertEquals(ToolkitLayout.defaultEnabledTools(), restored.enabledToolkitTools)
        assertTrue(restored.toolkitTabVisible)
        assertEquals(QuickStartLayout.SELECTION_COUNT, restored.quickStartTargets.size)
    }

    @Test
    fun spaceToggle_doesNotRequireValidQuickStartMidEdit() {
        val incompleteQs = OnboardingDraft(
            enableToolkit = true,
            quickStartTargets = listOf(QuickStartTarget.Timer),
        )
        assertFalse(incompleteQs.canComplete)

        val toggled = incompleteQs.withToolEnabled(enableBreathing = false)
        assertFalse(toggled.enableBreathing)
        assertTrue(toggled.enableToolkit)
        // prune/normalize should refill Quick Start after the change
        assertEquals(QuickStartLayout.SELECTION_COUNT, toggled.quickStartTargets.size)
    }

    @Test
    fun deselectingAllToolkitTools_hidesTab_andCtaRestores() {
        var draft = OnboardingDraft()
        ToolkitToolId.entries.forEach { id ->
            if (id in draft.enabledToolkitTools) {
                draft = draft.toggleToolkitTool(id)
            }
        }
        assertFalse(draft.toolkitTabVisible)
        assertFalse(draft.enableToolkit)

        val restored = draft.enableToolkitAgain()
        assertTrue(restored.toolkitTabVisible)
        assertTrue(restored.enabledToolkitTools.isNotEmpty())
    }
}
