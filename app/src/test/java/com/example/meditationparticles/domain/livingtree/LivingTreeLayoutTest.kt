package com.example.meditationparticles.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

class LivingTreeLayoutTest {
    private val canvas = 360f

    @Test
    fun computeNodeRadius_fewPeople_areLargerThanMany() {
        val few = LivingTreeLayout.computeNodeRadius(canvas, 3)
        val many = LivingTreeLayout.computeNodeRadius(canvas, 50)

        assertTrue(few > many)
        assertTrue(few >= 36f)
        assertTrue(many >= LivingTreeLayout.absoluteMinNodeRadius)
    }

    @Test
    fun computeNodeRadius_respectsMinAndMaxCaps() {
        val tinyCanvas = LivingTreeLayout.computeNodeRadius(200f, 3)
        val hugeCanvas = LivingTreeLayout.computeNodeRadius(900f, 3)

        assertTrue(tinyCanvas >= LivingTreeLayout.MIN_NODE_RADIUS)
        assertTrue(hugeCanvas <= LivingTreeLayout.MAX_NODE_RADIUS)
    }

    @Test
    fun computeCenterRadius_staysLargerThanSatellites() {
        val nodeRadius = LivingTreeLayout.computeNodeRadius(canvas, 12)
        val centerRadius = LivingTreeLayout.computeCenterRadius(canvas, nodeRadius)

        assertTrue(centerRadius > nodeRadius)
        assertTrue(centerRadius >= LivingTreeLayout.MIN_CENTER_RADIUS)
        assertTrue(centerRadius >= nodeRadius * LivingTreeLayout.CENTER_TO_NODE_RATIO * 0.95f)
    }

    @Test
    fun computeOrbitRadius_fewPeople_usesMoreCanvas() {
        val nodeRadius = LivingTreeLayout.computeNodeRadius(canvas, 4)
        val fewOrbit = LivingTreeLayout.computeOrbitRadius(canvas, 4, nodeRadius)
        val manyOrbit = LivingTreeLayout.computeOrbitRadius(canvas, 50, nodeRadius)
        val maxOrbit = LivingTreeLayout.maxOrbitRadius(canvas, nodeRadius)

        assertTrue(fewOrbit > manyOrbit * 0.9f)
        assertTrue(fewOrbit <= maxOrbit)
        assertTrue(manyOrbit <= maxOrbit)
    }

    @Test
    fun minOrbitRadiusForSpacing_growsAsCountIncreases() {
        val nodeRadius = 20f
        val spacing10 = LivingTreeLayout.minOrbitRadiusForSpacing(10, nodeRadius)
        val spacing50 = LivingTreeLayout.minOrbitRadiusForSpacing(50, nodeRadius)

        assertTrue(spacing50 > spacing10)
    }

    @Test
    fun computeLayoutSizing_manyPeople_shrinksNodesToReduceOverlap() {
        val aesthetic = LivingTreeLayout.computeNodeRadius(canvas, 50)
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, 50)
        val maxOrbit = LivingTreeLayout.maxOrbitRadius(canvas, sizing.nodeRadius)

        assertTrue(sizing.nodeRadius <= aesthetic + 0.5f)
        assertTrue(sizing.nodeRadius > 0f)
        assertTrue(sizing.orbitRadius <= maxOrbit + 0.5f)
        assertTrue(sizing.orbitRadius > canvas * 0.25f)
    }

    @Test
    fun radialPositions_evenlySpacedAroundRing() {
        val positions = LivingTreeLayout.radialPositions(
            personIds = listOf(1L, 2L, 3L),
            centerX = 100f,
            centerY = 100f,
            orbitRadius = 80f,
            nodeRadius = 24f,
        )

        assertEquals(3, positions.size)
        assertEquals(180f, positions[0].x, 0.01f)
        assertEquals(100f, positions[0].y, 0.01f)
        positions.forEach { assertEquals(24f, it.radius, 0.01f) }
    }

    @Test
    fun radialPositions_nullStoredAngles_useEvenSpacingNotZero() {
        val ids = listOf(10L, 20L, 30L)
        val stored = LivingTreeLayout.storedAnglesFromPeople(
            ids.map { it to null },
        )
        assertTrue(stored.isEmpty())

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = 0f,
            centerY = 0f,
            orbitRadius = 100f,
            nodeRadius = 20f,
            storedAngles = stored,
        )

        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(3, distinct.size)
    }

    @Test
    fun storedAnglesFromPeople_omitsNullDbValues() {
        val stored = LivingTreeLayout.storedAnglesFromPeople(
            listOf(1L to 1.5, 2L to null, 3L to 0.0),
        )

        assertEquals(2, stored.size)
        assertEquals(1.5, stored[1L]!!, 0.0001)
        assertEquals(0.0, stored[3L]!!, 0.0001)
        assertTrue(2L !in stored)
    }

    @Test
    fun angularSpacingRadians_dividesFullCircleByCount() {
        val spacing = LivingTreeLayout.angularSpacingRadians(8)
        assertEquals(PI / 4.0, spacing, 0.0001)
    }

    @Test
    fun adjacentChord_atComputedOrbit_avoidsOverlapForModerateCounts() {
        val count = 20
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, count)
        val chord = 2.0 * sizing.orbitRadius * sin(PI / count)
        val required = 2.0 * sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP

        assertTrue(chord >= required - 1.0)
    }

    @Test
    fun computeLayoutSizing_smallGroups_haveComfortableBubbleSizes() {
        listOf(5 to 36f, 8 to 28f, 10 to 28f).forEach { (count, minRadius) ->
            val sizing = LivingTreeLayout.computeLayoutSizing(canvas, count)
            assertTrue(
                "Expected comfortable satellites for $count people",
                sizing.nodeRadius >= minRadius,
            )
            assertNoPairwiseOverlap(count, sizing)
        }
    }

    @Test
    fun computeLayoutSizing_largeGroups_avoidOverlap() {
        listOf(30, 45, 50).forEach { count ->
            val sizing = LivingTreeLayout.computeLayoutSizing(canvas, count)
            assertNoPairwiseOverlap(count, sizing)
        }
    }

    private fun assertNoPairwiseOverlap(count: Int, sizing: LivingTreeLayout.LayoutSizing) {
        val ids = (1L..count.toLong()).toList()
        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = canvas / 2f,
            centerY = canvas / 2f,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
        )
        val minSeparation = 2f * sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP - 1f
        for (i in positions.indices) {
            for (j in i + 1 until positions.size) {
                val a = positions[i]
                val b = positions[j]
                val distance = hypot(
                    (a.x - b.x).toDouble(),
                    (a.y - b.y).toDouble(),
                )
                assertTrue(
                    "Nodes ${a.id} and ${b.id} overlap at count=$count (dist=$distance)",
                    distance >= minSeparation,
                )
            }
        }
    }
}
