package com.example.meditationparticles.domain.affirmations

import com.example.meditationparticles.data.local.AffirmationEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AffirmationReorderTest {
    @Test
    fun reorder_movesItemToTargetIndex() {
        val original = listOf(
            AffirmationEntity(id = 1, text = "A", sortOrder = 0),
            AffirmationEntity(id = 2, text = "B", sortOrder = 1),
            AffirmationEntity(id = 3, text = "C", sortOrder = 2),
        )

        val reordered = AffirmationReorder.reorder(original, fromIndex = 0, toIndex = 2)

        assertEquals(listOf("B", "C", "A"), reordered.map { it.text })
    }

    @Test
    fun reorder_returnsOriginalWhenIndexesInvalid() {
        val original = listOf(
            AffirmationEntity(id = 1, text = "A", sortOrder = 0),
            AffirmationEntity(id = 2, text = "B", sortOrder = 1),
        )

        assertEquals(original, AffirmationReorder.reorder(original, fromIndex = -1, toIndex = 0))
        assertEquals(original, AffirmationReorder.reorder(original, fromIndex = 0, toIndex = 0))
    }
}
