package com.example.meditationparticles.domain.toolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolkitLayoutTest {
    @Test
    fun orderedTools_respectsSavedOrderAndEnabledFilter() {
        val order = listOf(
            ToolkitToolId.MicroPause,
            ToolkitToolId.ThoughtDump,
            ToolkitToolId.BoundarySetting,
            ToolkitToolId.FutureSelfMessage,
        )
        val enabled = setOf(ToolkitToolId.ThoughtDump, ToolkitToolId.MicroPause)

        val tools = ToolkitLayout.orderedTools(
            category = ToolkitCategory.Proactive,
            enabledIds = enabled,
            savedOrder = order,
        )

        assertEquals(listOf(ToolkitToolId.MicroPause, ToolkitToolId.ThoughtDump), tools.map { it.id })
    }

    @Test
    fun reorder_movesItemToTargetIndex() {
        val original = listOf(
            ToolkitToolId.ThoughtDump,
            ToolkitToolId.BoundarySetting,
            ToolkitToolId.MicroPause,
            ToolkitToolId.FutureSelfMessage,
        )
        val reordered = ToolkitLayout.reorder(original, fromIndex = 0, toIndex = 2)
        assertEquals(
            listOf(
                ToolkitToolId.BoundarySetting,
                ToolkitToolId.MicroPause,
                ToolkitToolId.ThoughtDump,
                ToolkitToolId.FutureSelfMessage,
            ),
            reordered,
        )
    }

    @Test
    fun normalizeOrder_appendsMissingDefaults() {
        val normalized = ToolkitLayout.normalizeOrder(
            ToolkitCategory.Reactive,
            listOf(ToolkitToolId.AnxietyLog, ToolkitToolId.Grounding54321),
        )

        assertEquals(ToolkitToolId.AnxietyLog, normalized.first())
        assertTrue(ToolkitToolId.LovingKindness in normalized)
        assertEquals(ToolkitLayout.defaultOrder(ToolkitCategory.Reactive).size, normalized.size)
    }

    @Test
    fun randomReactive_onlyPicksEnabledTools() {
        val enabled = setOf(ToolkitToolId.Grounding54321)
        repeat(20) {
            val tool = ToolkitLayout.randomReactive(enabled)
            assertEquals(ToolkitToolId.Grounding54321, tool?.id)
        }
    }

    @Test
    fun randomReactive_returnsNullWhenNoneEnabled() {
        assertEquals(null, ToolkitLayout.randomReactive(emptySet()))
    }
}
