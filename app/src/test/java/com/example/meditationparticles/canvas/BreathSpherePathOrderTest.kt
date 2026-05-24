package com.example.meditationparticles.canvas

import com.example.meditationparticles.breathing.test.BreathTestFixtures
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.computeStructureSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathSpherePathOrderTest {

    @Test
    fun boxInhalePath_runsBottomToTopSpatially() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val yCoords = layout.inhalePath.map { requireNotNull(layout.sphere(it)).center.y }

        assertEquals(yCoords.sortedDescending(), yCoords)
    }

    @Test
    fun boxExhalePath_runsTopToBottomSpatially() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val yCoords = layout.exhalePath.map { requireNotNull(layout.sphere(it)).center.y }

        assertEquals(yCoords.sorted(), yCoords)
    }

    @Test
    fun allPatterns_pathsAreUniqueAndSpatiallyOrdered() {
        BreathingPattern.All.forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            val spec = computeStructureSpec(pattern)

            assertEquals(
                "${pattern.name} inhale path length",
                spec.inhaleSphereCount,
                layout.inhalePath.size,
            )
            assertEquals(
                "${pattern.name} exhale path length",
                spec.exhaleSphereCount,
                layout.exhalePath.size,
            )
            assertEquals(
                "${pattern.name} inhale path has unique ids",
                spec.inhaleSphereCount,
                layout.inhalePath.toSet().size,
            )
            assertEquals(
                "${pattern.name} exhale path has unique ids",
                spec.exhaleSphereCount,
                layout.exhalePath.toSet().size,
            )
            assertTrue(
                "${pattern.name} inhale/exhale paths do not overlap",
                layout.inhalePath.toSet().intersect(layout.exhalePath.toSet()).isEmpty(),
            )

            if (layout.inhalePath.size > 1) {
                val inhaleY = layout.inhalePath.map { requireNotNull(layout.sphere(it)).center.y }
                assertEquals("${pattern.name} inhale bottom-to-top", inhaleY.sortedDescending(), inhaleY)
            }

            if (layout.exhalePath.size > 1) {
                val exhaleY = layout.exhalePath.map { requireNotNull(layout.sphere(it)).center.y }
                assertEquals("${pattern.name} exhale top-to-bottom", exhaleY.sorted(), exhaleY)
            }
        }
    }
}
