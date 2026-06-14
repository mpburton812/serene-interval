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
        assertTrue(few >= LivingTreeLayout.MAX_NODE_RADIUS * 0.9f)
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
        assertTrue(centerRadius >= nodeRadius * LivingTreeLayout.CENTER_TO_NODE_RATIO * 0.99f)
    }

    @Test
    fun computeOrbitRadius_fewPeople_usesMoreCanvas() {
        val fewSizing = LivingTreeLayout.computeLayoutSizing(canvas, 4)
        val manySizing = LivingTreeLayout.computeLayoutSizing(canvas, 50)
        val fewMaxOrbit = LivingTreeLayout.maxOrbitRadius(canvas, fewSizing.nodeRadius)
        val manyMaxOrbit = LivingTreeLayout.maxOrbitRadius(canvas, manySizing.nodeRadius)

        assertTrue(fewSizing.orbitRadius / fewMaxOrbit >= manySizing.orbitRadius / manyMaxOrbit * 0.85f)
        assertTrue(fewSizing.orbitRadius <= fewMaxOrbit)
        assertTrue(manySizing.orbitRadius <= manyMaxOrbit)
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
        assertTrue(sizing.nodeRadius >= LivingTreeLayout.absoluteMinNodeRadius)
        assertTrue(sizing.orbitRadius <= maxOrbit + 0.5f)
        assertTrue(sizing.orbitRadius > 0f)
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
        val ids = List(7) { index -> (index + 1).toLong() }
        val stored = LivingTreeLayout.storedPositionsFromPeople(
            ids.map { Triple(it, null, null) },
        )
        assertTrue(stored.isEmpty())

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = 0f,
            centerY = 0f,
            orbitRadius = 100f,
            nodeRadius = 20f,
            storedPositions = stored,
        )

        assertEquals(7, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(7, distinct.size)
    }

    @Test
    fun radialPositions_duplicateStoredAngles_reSpreadEvenly() {
        val ids = List(7) { index -> (index + 1).toLong() }
        val stored = ids.associateWith {
            LivingTreeLayout.StoredPosition(angleRadians = 0.0, radiusFraction = 1.0)
        }

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = 0f,
            centerY = 0f,
            orbitRadius = 100f,
            nodeRadius = 20f,
            storedPositions = stored,
        )

        assertEquals(7, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(7, distinct.size)
    }

    @Test
    fun radialPositions_filteredSubset_evenlySpacesVisiblePeople() {
        val visibleIds = listOf(1L, 3L, 5L, 7L, 9L, 11L, 13L)
        val stored = LivingTreeLayout.storedPositionsFromPeople(
            visibleIds.map { Triple(it, 0.0, 1.0) },
        )

        val positions = LivingTreeLayout.radialPositions(
            personIds = visibleIds,
            centerX = 50f,
            centerY = 50f,
            orbitRadius = 80f,
            nodeRadius = 16f,
            storedPositions = stored,
        )

        assertEquals(7, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(7, distinct.size)
    }

    @Test
    fun storedPositionsFromPeople_omitsNullDbValues() {
        val stored = LivingTreeLayout.storedPositionsFromPeople(
            listOf(
                Triple(1L, 1.5, 0.8),
                Triple(2L, null, null),
                Triple(3L, 0.0, 1.0),
            ),
        )

        assertEquals(2, stored.size)
        assertEquals(1.5, stored[1L]!!.angleRadians, 0.0001)
        assertEquals(0.8, stored[1L]!!.radiusFraction, 0.0001)
        assertEquals(0.0, stored[3L]!!.angleRadians, 0.0001)
        assertTrue(2L !in stored)
    }

    @Test
    fun angleForNewPerson_returnsNullSoLayoutEvenlySpaces() {
        assertEquals(null, LivingTreeLayout.angleForNewPerson(0))
        assertEquals(null, LivingTreeLayout.angleForNewPerson(7))
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
        val manySizing = LivingTreeLayout.computeLayoutSizing(canvas, 50)
        val oneSizing = LivingTreeLayout.computeLayoutSizing(canvas, 1)
        val threeSizing = LivingTreeLayout.computeLayoutSizing(canvas, 3)
        assertTrue(oneSizing.nodeRadius >= threeSizing.nodeRadius * 0.9f)
        assertTrue(threeSizing.nodeRadius >= manySizing.nodeRadius * 2f)
        listOf(5, 8, 10).forEach { count ->
            val sizing = LivingTreeLayout.computeLayoutSizing(canvas, count)
            assertTrue(
                "Expected larger satellites for $count people than for 50",
                sizing.nodeRadius > manySizing.nodeRadius * 1.4f,
            )
            assertNoOverlap(count, sizing)
        }
    }

    @Test
    fun computeLayoutSizing_largeGroups_avoidOverlap() {
        val phoneCanvas = 1080f
        listOf(30, 45, 50).forEach { count ->
            val sizing = LivingTreeLayout.computeLayoutSizing(phoneCanvas, count)
            assertNoOverlap(count, sizing, canvasDim = phoneCanvas)
        }
    }

    @Test
    fun radialPositions_persistedOverlap_reSpreadEvenly() {
        val ids = List(7) { index -> (index + 1).toLong() }
        val stored = ids.associateWith {
            LivingTreeLayout.StoredPosition(angleRadians = 0.5, radiusFraction = 0.35)
        }
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, ids.size)

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = canvas / 2f,
            centerY = canvas / 2f,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
            storedPositions = stored,
        )

        assertEquals(7, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(7, distinct.size)
        assertNoOverlap(ids.size, sizing, positions)
    }

    @Test
    fun bubbleGap_isTenPixels() {
        assertEquals(10f, LivingTreeLayout.BUBBLE_GAP, 0.001f)
    }

    @Test
    fun computeLayoutSizing_centerShrinksWithNodesForLargeGroups() {
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, 50)
        assertTrue(sizing.centerRadius >= sizing.nodeRadius * LivingTreeLayout.CENTER_TO_NODE_RATIO * 0.99f)
        assertTrue(sizing.centerRadius <= LivingTreeLayout.MAX_CENTER_RADIUS)
    }

    @Test
    fun resolveStoredPositions_userPlacedOverlap_isNotRepacked() {
        val ids = listOf(1L, 2L, 3L)
        val overlapAngle = 1.1
        val stored = mapOf(
            1L to LivingTreeLayout.StoredPosition(angleRadians = overlapAngle, radiusFraction = 1.0),
            2L to LivingTreeLayout.StoredPosition(angleRadians = overlapAngle, radiusFraction = 1.0),
            3L to LivingTreeLayout.StoredPosition(angleRadians = 2.5, radiusFraction = 1.0),
        )
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, ids.size)

        val resolved = LivingTreeLayout.resolveStoredPositions(
            personIds = ids,
            stored = stored,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
            userPlacedIds = setOf(2L),
        )

        assertEquals(overlapAngle, resolved[2L]!!.angleRadians, 0.0001)
        assertEquals(1.0, resolved[2L]!!.radiusFraction, 0.0001)
    }

    @Test
    fun computeLayoutSizing_usesTotalCountNotFilteredSubset() {
        val fullSizing = LivingTreeLayout.computeLayoutSizing(canvas, 40)
        val filteredSizing = LivingTreeLayout.computeLayoutSizing(canvas, 4)
        assertTrue(fullSizing.nodeRadius < filteredSizing.nodeRadius)
    }

    @Test
    fun nudgeStoredPosition_avoidsCenterAndPeerOverlap() {
        val ids = listOf(1L, 2L, 3L)
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, ids.size)
        val centerX = canvas / 2f
        val centerY = canvas / 2f
        val others = mapOf(
            2L to LivingTreeLayout.StoredPosition(angleRadians = PI / 2, radiusFraction = 1.0),
            3L to LivingTreeLayout.StoredPosition(angleRadians = PI, radiusFraction = 1.0),
        )
        val proposed = LivingTreeLayout.StoredPosition(angleRadians = 0.0, radiusFraction = 0.35)

        val nudged = LivingTreeLayout.nudgeStoredPosition(
            draggedId = 1L,
            proposed = proposed,
            otherStored = others,
            personIds = ids,
            centerX = centerX,
            centerY = centerY,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = centerX,
            centerY = centerY,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
            storedPositions = others + (1L to nudged),
        )
        assertNoOverlap(ids.size, sizing, positions)
    }

    private fun assertNoOverlap(
        count: Int,
        sizing: LivingTreeLayout.LayoutSizing,
        positions: List<LivingTreeLayout.NodePosition>? = null,
        canvasDim: Float = canvas,
    ) {
        val ids = (1L..count.toLong()).toList()
        val nodes = positions ?: LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = canvasDim / 2f,
            centerY = canvasDim / 2f,
            orbitRadius = sizing.orbitRadius,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )
        val minSeparation = 2f * sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP - 1f
        val centerSeparation = sizing.centerRadius + sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP - 1f
        val centerX = canvasDim / 2f
        val centerY = canvasDim / 2f
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i]
                val b = nodes[j]
                val distance = hypot(
                    (a.x - b.x).toDouble(),
                    (a.y - b.y).toDouble(),
                )
                assertTrue(
                    "Nodes ${a.id} and ${b.id} overlap at count=$count (dist=$distance)",
                    distance >= minSeparation,
                )
            }
            val node = nodes[i]
            val centerDistance = hypot(
                (node.x - centerX).toDouble(),
                (node.y - centerY).toDouble(),
            )
            assertTrue(
                "Node ${node.id} overlaps center at count=$count (dist=$centerDistance)",
                centerDistance >= centerSeparation,
            )
        }
    }
}
