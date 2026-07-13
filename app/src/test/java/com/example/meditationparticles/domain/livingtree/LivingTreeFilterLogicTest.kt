package com.example.meditationparticles.domain.livingtree

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeFilterLogicTest {
    @Test
    fun visiblePersonIds_withNoSelection_returnsEveryone() {
        val people = mapOf(
            1L to setOf(10L),
            2L to setOf(20L),
        )

        assertEquals(setOf(1L, 2L), LivingTreeFilterLogic.visiblePersonIds(people, emptySet()))
    }

    @Test
    fun visiblePersonIds_withSelection_usesAndLogic() {
        val people = mapOf(
            1L to setOf(10L),
            2L to setOf(20L),
            3L to setOf(10L, 20L),
            4L to setOf(30L),
            5L to setOf(10L, 20L, 30L),
        )

        assertEquals(
            setOf(3L, 5L),
            LivingTreeFilterLogic.visiblePersonIds(people, setOf(10L, 20L)),
        )
    }

    @Test
    fun matchedTagIds_preservesSelectedOrder() {
        val matched = LivingTreeFilterLogic.matchedTagIds(
            personTagIds = setOf(2L, 3L, 4L),
            selectedTagIds = listOf(4L, 1L, 2L),
        )

        assertEquals(listOf(4L, 2L), matched)
    }

    @Test
    fun bubbleColors_splitsByMatchedTags() {
        val colors = LivingTreeFilterLogic.bubbleColors(
            personTagIds = setOf(1L, 2L, 3L),
            selectedTagIds = listOf(2L, 3L, 4L),
            tagColors = mapOf(
                2L to 0xFF112233.toInt(),
                3L to 0xFF445566.toInt(),
            ),
        )

        assertEquals(2, colors.size)
        assertEquals(Color(0xFF112233), colors[0])
        assertEquals(Color(0xFF445566), colors[1])
    }

    @Test
    fun tagPersonCounts_countsDistinctPeoplePerTag() {
        val counts = LivingTreeFilterLogic.tagPersonCounts(
            peopleTagIds = mapOf(
                1L to setOf(10L, 20L),
                2L to setOf(10L),
                3L to setOf(30L),
            ),
            tagIds = listOf(10L, 20L, 30L),
        )

        assertEquals(2, counts[10L])
        assertEquals(1, counts[20L])
        assertEquals(1, counts[30L])
    }

    @Test
    fun defaultTags_includeQueerAndTransInclusiveSet() {
        val names = LivingTreeDefaults.defaultTags.map { it.name }

        assertTrue(names.contains("Chosen Family"))
        assertTrue(names.contains("Trans"))
        assertTrue(names.contains("Queer"))
        assertTrue(names.contains("Poly"))
        assertTrue(names.contains("Long Distance"))
    }
}
