package com.example.meditationparticles.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class LivingTreeLayoutTest {
    private val canvas = 360f

    @Test
    fun computeNodeRadius_fewPeople_areLargerThanMany() {
        val few = LivingTreeLayout.computeNodeRadius(canvas, 3)
        val many = LivingTreeLayout.computeNodeRadius(canvas, 50)

        assertTrue(few > many)
        assertTrue(few >= 30f)
        assertTrue(many <= LivingTreeLayout.MIN_NODE_RADIUS + 6f)
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
        assertTrue(sizing.nodeRadius >= LivingTreeLayout.MIN_NODE_RADIUS - 0.5f)
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
}
