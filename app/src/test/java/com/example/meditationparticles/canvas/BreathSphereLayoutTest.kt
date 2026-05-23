package com.example.meditationparticles.canvas

import com.example.meditationparticles.domain.breathing.BreathingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathSphereLayoutTest {

    @Test
    fun boxBreathing_ladderHasTenSpheresAndDistinctPaths() {
        val layout = computeStructureLayout(
            pattern = BreathingPattern.BoxBreathing,
            width = 1080f,
            height = 2200f,
            topInset = 120f,
            bottomInset = 160f,
        )

        assertEquals(LayoutMode.Ladder, layout.layoutMode)
        assertEquals(10, layout.allSpheres.size)
        assertEquals(4, layout.inhalePath.size)
        assertEquals(4, layout.exhalePath.size)
        assertEquals(4, layout.inhalePath.toSet().size)
        assertEquals(4, layout.exhalePath.toSet().size)
        assertTrue(layout.inhalePath.toSet().intersect(layout.exhalePath.toSet()).isEmpty())
    }
}
