package com.example.meditationparticles.canvas

import com.example.meditationparticles.breathing.test.BreathTestFixtures
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.computeStructureSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathSphereLayoutTest {

    @Test
    fun boxBreathing_ladderHasTenSpheresAndDistinctPaths() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)

        assertEquals(LayoutMode.InterleavedLadder, layout.layoutMode)
        assertEquals(10, layout.allSpheres.size)
        assertEquals(4, layout.inhalePath.size)
        assertEquals(4, layout.exhalePath.size)
        assertTrue(layout.inhalePath.toSet().intersect(layout.exhalePath.toSet()).isEmpty())
    }

    @Test
    fun allPatterns_produceLayoutsMatchingSpecCounts() {
        BreathingPattern.All.forEach { pattern ->
            val spec = computeStructureSpec(pattern)
            val layout = BreathTestFixtures.layoutFor(pattern)

            assertEquals(spec.inhaleSphereCount, layout.inhalePath.size)
            assertEquals(spec.exhaleSphereCount, layout.exhalePath.size)
            assertEquals(spec.hasTopHold, layout.topHoldId != null)
            assertEquals(spec.hasBottomHold, layout.bottomHoldId != null)

            val expectedSpheres = spec.inhaleSphereCount + spec.exhaleSphereCount +
                (if (spec.hasTopHold) 1 else 0) + (if (spec.hasBottomHold) 1 else 0)
            assertEquals("${pattern.name} sphere count", expectedSpheres, layout.allSpheres.size)
        }
    }

    @Test
    fun allPatterns_fitPhoneMediumAndLargeScreens() {
        val sizes = listOf(
            BreathTestFixtures.SMALL_WIDTH to BreathTestFixtures.SMALL_HEIGHT,
            BreathTestFixtures.PHONE_WIDTH to BreathTestFixtures.PHONE_HEIGHT,
            BreathTestFixtures.LARGE_WIDTH to BreathTestFixtures.LARGE_HEIGHT,
        )

        BreathingPattern.All.forEach { pattern ->
            sizes.forEach { (width, height) ->
                val layout = BreathTestFixtures.layoutFor(pattern, width = width, height = height)
                assertTrue(
                    "${pattern.name} at ${width.toInt()}x${height.toInt()} has spheres",
                    layout.allSpheres.isNotEmpty(),
                )
                layout.allSpheres.forEach { sphere ->
                    assertTrue(sphere.radius >= 8f)
                    assertTrue(sphere.center.x in 0f..width)
                    assertTrue(sphere.center.y in BreathTestFixtures.TOP_INSET..(height - BreathTestFixtures.BOTTOM_INSET))
                }
            }
        }
    }

    @Test
    fun resonant_hasTenSpheresWithoutHolds() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.Resonant)

        assertEquals(10, layout.allSpheres.size)
        assertEquals(null, layout.topHoldId)
        assertEquals(null, layout.bottomHoldId)
    }

    @Test
    fun fourSevenEight_hasThirteenSpheresWithTopHoldOnly() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.FourSevenEight)

        assertEquals(13, layout.allSpheres.size)
        assertTrue(layout.topHoldId != null)
        assertEquals(null, layout.bottomHoldId)
    }

    @Test
    fun fourSevenEight_usesFlowChainWithDirectHoldConnections() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.FourSevenEight)

        assertEquals(LayoutMode.FlowChain, layout.layoutMode)
        assertTrue(layout.pipes.size >= layout.allSpheres.size)

        val lastInhale = requireNotNull(layout.sphere(layout.inhalePath.last()))
        val topHold = requireNotNull(layout.sphere(layout.topHoldId!!))
        val firstExhale = requireNotNull(layout.sphere(layout.exhalePath.first()))

        assertTrue(topHold.center.y < lastInhale.center.y)
        assertTrue(firstExhale.center.y > topHold.center.y)

        val inhaleToHoldGap = lastInhale.center.y - topHold.center.y -
            lastInhale.radius - topHold.radius
        val holdToExhaleGap = firstExhale.center.y - topHold.center.y -
            topHold.radius - firstExhale.radius

        assertTrue("Last inhale should connect directly to hold", inhaleToHoldGap in 4f..16f)
        assertTrue("Hold should connect directly to first exhale", holdToExhaleGap in 4f..16f)
    }

    @Test
    fun flowChainPatterns_fillMostOfVerticalCanvas() {
        val zoneHeight = BreathTestFixtures.PHONE_HEIGHT -
            BreathTestFixtures.TOP_INSET - BreathTestFixtures.BOTTOM_INSET - 20f

        listOf(
            BreathingPattern.FourSevenEight,
            BreathingPattern.Tactical,
            BreathingPattern.PhysiologicalSigh,
        ).forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            val top = layout.allSpheres.minOf { it.center.y - it.radius }
            val bottom = layout.allSpheres.maxOf { it.center.y + it.radius }
            val spanRatio = (bottom - top) / zoneHeight
            assertTrue(
                "${pattern.name} span ratio $spanRatio",
                spanRatio >= 0.75f,
            )
            assertTrue(
                "${pattern.name} avg radius",
                layout.allSpheres.map { it.radius }.average() >= 20.0,
            )
        }
    }

    @Test
    fun nonBoxAsymmetricPatterns_useFlowChain() {
        listOf(
            BreathingPattern.FourSevenEight,
            BreathingPattern.Tactical,
            BreathingPattern.PhysiologicalSigh,
        ).forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            assertEquals("${pattern.name} layout", LayoutMode.FlowChain, layout.layoutMode)
            assertTrue(
                "${pattern.name} pipe count",
                layout.pipes.size >= layout.allSpheres.size,
            )
        }
    }

    @Test
    fun resonant_closesFullCircuitAtTopAndBottom() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.Resonant)

        assertTrue(BreathTestFixtures.hasClosedBreathCircuit(layout))
        assertTrue(
            BreathTestFixtures.pipesConnect(
                layout,
                layout.inhalePath.last(),
                layout.exhalePath.first(),
            ),
        )
        assertTrue(
            BreathTestFixtures.pipesConnect(
                layout,
                layout.exhalePath.last(),
                layout.inhalePath.first(),
            ),
        )
    }

    @Test
    fun allPatterns_closeBreathCircuitWherePossible() {
        BreathingPattern.All.forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            assertTrue(
                "${pattern.name} should close the breath loop",
                BreathTestFixtures.hasClosedBreathCircuit(layout),
            )
        }
    }
}
