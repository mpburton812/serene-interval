package com.example.meditationparticles.domain.toolkit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolkitLayoutTest {
    @Test
    fun orderedTools_respectsSavedOrderAndEnabledFilter() {
        val order = listOf(
            ToolkitToolId.FutureSelfMessage,
            ToolkitToolId.LovingKindness,
            ToolkitToolId.ThoughtDump,
        )
        val enabled = setOf(ToolkitToolId.ThoughtDump, ToolkitToolId.LovingKindness)

        val tools = ToolkitLayout.orderedTools(
            category = ToolkitCategory.Proactive,
            enabledIds = enabled,
            savedOrder = order,
        )

        assertEquals(listOf(ToolkitToolId.LovingKindness, ToolkitToolId.ThoughtDump), tools.map { it.id })
    }

    @Test
    fun orderedTools_sortsByUsageCountDescending() {
        val savedOrder = ToolkitLayout.defaultOrder(ToolkitCategory.Reactive)
        val enabled = setOf(
            ToolkitToolId.Grounding54321,
            ToolkitToolId.AnxietyLog,
            ToolkitToolId.MuscleRelaxation,
        )
        val usageCounts = mapOf(
            ToolkitToolId.AnxietyLog to 10,
            ToolkitToolId.Grounding54321 to 3,
            ToolkitToolId.MuscleRelaxation to 1,
        )

        val tools = ToolkitLayout.orderedTools(
            category = ToolkitCategory.Reactive,
            enabledIds = enabled,
            savedOrder = savedOrder,
            usageCounts = usageCounts,
        )

        assertEquals(
            listOf(
                ToolkitToolId.AnxietyLog,
                ToolkitToolId.Grounding54321,
                ToolkitToolId.MuscleRelaxation,
            ),
            tools.map { it.id },
        )
    }

    @Test
    fun sortByUsage_tiebreaksWithSavedOrder() {
        val savedOrder = listOf(
            ToolkitToolId.MicroPause,
            ToolkitToolId.ThoughtDump,
            ToolkitToolId.BoundarySetting,
        )
        val toolIds = listOf(
            ToolkitToolId.ThoughtDump,
            ToolkitToolId.MicroPause,
            ToolkitToolId.BoundarySetting,
        )

        val sorted = ToolkitLayout.sortByUsage(
            toolIds = toolIds,
            category = ToolkitCategory.Reactive,
            savedOrder = savedOrder,
            usageCounts = emptyMap(),
        )

        assertEquals(savedOrder, sorted)
    }

    @Test
    fun sortByUsage_equalCountsPreserveSavedOrder() {
        val savedOrder = listOf(
            ToolkitToolId.Grounding54321,
            ToolkitToolId.AnxietyLog,
        )
        val toolIds = listOf(
            ToolkitToolId.AnxietyLog,
            ToolkitToolId.Grounding54321,
        )
        val usageCounts = mapOf(
            ToolkitToolId.AnxietyLog to 2,
            ToolkitToolId.Grounding54321 to 2,
        )

        val sorted = ToolkitLayout.sortByUsage(
            toolIds = toolIds,
            category = ToolkitCategory.Reactive,
            savedOrder = savedOrder,
            usageCounts = usageCounts,
        )

        assertEquals(savedOrder, sorted)
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
        assertTrue(ToolkitToolId.MicroPause in normalized)
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
