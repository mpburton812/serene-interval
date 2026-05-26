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
        assertTrue(
            "4-7-8 pipe count",
            layout.pipes.size >= layout.allSpheres.size - 1,
        )

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
                layout.pipes.size >= layout.allSpheres.size - 2,
            )
        }
    }

    @Test
    fun allPatterns_pipesOnlyConnectAdjacentSpheres() {
        BreathingPattern.All.forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            val maxRadius = layout.allSpheres.maxOf { it.radius }
            layout.pipes.forEach { pipe ->
                val edgePoints = layout.pipeEdgePoints(pipe) ?: return@forEach
                val (a, b) = edgePoints
                val length = kotlin.math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
                assertTrue(
                    "${pattern.name} pipe length $length should stay local",
                    length <= maxRadius * 7f,
                )
            }
        }
    }

    @Test
    fun boxBreathing_bottomHoldConnectsToLastRow() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)

        assertTrue(
            BreathTestFixtures.pipesConnect(
                layout,
                layout.bottomHoldId!!,
                layout.inhalePath.first(),
            ),
        )
    }

    @Test
    fun modeB_resonant_hasTwoSpheresWithHorizontalPipe() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.Resonant)

        assertEquals(LayoutMode.ModeB, layout.layoutMode)
        assertEquals(2, layout.allSpheres.size)
        assertEquals(null, layout.topHoldId)
        assertEquals(null, layout.bottomHoldId)
        assertEquals(1, layout.inhalePath.size)
        assertEquals(1, layout.exhalePath.size)
        assertEquals(1, layout.pipes.size)

        val inhale = requireNotNull(layout.sphere(layout.inhalePath.first()))
        val exhale = requireNotNull(layout.sphere(layout.exhalePath.first()))
        assertTrue(inhale.center.x < exhale.center.x)
        assertTrue(inhale.role.name.contains("Inhale"))
        assertTrue(exhale.role.name.contains("Exhale"))
    }

    @Test
    fun modeB_fourSevenEight_hasThreeSphereDiamondWithoutBottomHold() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.FourSevenEight)

        assertEquals(LayoutMode.ModeB, layout.layoutMode)
        assertEquals(3, layout.allSpheres.size)
        assertTrue(layout.topHoldId != null)
        assertEquals(null, layout.bottomHoldId)

        val inhale = requireNotNull(layout.sphere(layout.inhalePath.first()))
        val exhale = requireNotNull(layout.sphere(layout.exhalePath.first()))
        val topHold = requireNotNull(layout.sphere(layout.topHoldId!!))

        assertTrue(topHold.center.y < inhale.center.y)
        assertTrue(topHold.center.y < exhale.center.y)
        assertTrue(inhale.center.x < topHold.center.x)
        assertTrue(exhale.center.x > topHold.center.x)
    }

    @Test
    fun modeB_boxBreathing_hasFourSphereDiamond() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.BoxBreathing)

        assertEquals(LayoutMode.ModeB, layout.layoutMode)
        assertEquals(4, layout.allSpheres.size)
        assertTrue(layout.topHoldId != null)
        assertTrue(layout.bottomHoldId != null)

        val inhale = requireNotNull(layout.sphere(layout.inhalePath.first()))
        val exhale = requireNotNull(layout.sphere(layout.exhalePath.first()))
        val topHold = requireNotNull(layout.sphere(layout.topHoldId!!))
        val bottomHold = requireNotNull(layout.sphere(layout.bottomHoldId!!))

        assertTrue(topHold.center.y < inhale.center.y)
        assertTrue(bottomHold.center.y > exhale.center.y)
        assertTrue(inhale.center.x < exhale.center.x)
    }

    @Test
    fun modeB_physiologicalSigh_hasTwoSpheres() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.PhysiologicalSigh)

        assertEquals(LayoutMode.ModeB, layout.layoutMode)
        assertEquals(2, layout.allSpheres.size)
        assertEquals(1, layout.inhalePath.size)
        assertEquals(1, layout.exhalePath.size)
    }

    @Test
    fun modeB_allPatterns_fitPhoneMediumAndLargeScreens() {
        val sizes = listOf(
            BreathTestFixtures.SMALL_WIDTH to BreathTestFixtures.SMALL_HEIGHT,
            BreathTestFixtures.PHONE_WIDTH to BreathTestFixtures.PHONE_HEIGHT,
            BreathTestFixtures.LARGE_WIDTH to BreathTestFixtures.LARGE_HEIGHT,
        )

        BreathingPattern.All.forEach { pattern ->
            sizes.forEach { (width, height) ->
                val layout = BreathTestFixtures.layoutForModeB(pattern, width = width, height = height)
                assertTrue("${pattern.name} mode B has spheres", layout.allSpheres.isNotEmpty())
                layout.allSpheres.forEach { sphere ->
                    assertTrue(sphere.radius >= 8f)
                    assertTrue(sphere.center.x in 0f..width)
                    assertTrue(sphere.center.y in BreathTestFixtures.TOP_INSET..(height - BreathTestFixtures.BOTTOM_INSET))
                }
            }
        }
    }
}
