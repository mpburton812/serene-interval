package com.safehaven.affirmations.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

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
            centerRadius = 40f,
        )

        assertEquals(3, positions.size)
        assertEquals(180f, positions[0].x, 0.01f)
        assertEquals(100f, positions[0].y, 0.01f)
        positions.forEach { assertEquals(24f, it.radius, 0.01f) }
        assertNoOverlap(3, LivingTreeLayout.LayoutSizing(24f, 80f, 80f, 40f), positions)
    }

    @Test
    fun radialPositions_ellipticalOrbit_usesFullCanvasAxes() {
        val positions = LivingTreeLayout.radialPositions(
            personIds = listOf(1L, 2L, 3L, 4L),
            centerX = 200f,
            centerY = 300f,
            orbitRadiusX = 160f,
            orbitRadiusY = 240f,
            nodeRadius = 28f,
            centerRadius = 40f,
        )

        assertEquals(4, positions.size)
        // First slot at angle 0 → right along X axis
        assertEquals(360f, positions[0].x, 0.01f)
        assertEquals(300f, positions[0].y, 0.01f)
        // Second slot at π/2 → up along Y axis (taller orbit)
        assertEquals(200f, positions[1].x, 0.01f)
        assertEquals(540f, positions[1].y, 0.01f)

        val xExtents = positions.map { abs(it.x - 200f) }
        val yExtents = positions.map { abs(it.y - 300f) }
        assertTrue(yExtents.max() > xExtents.max())
    }

    @Test
    fun computeLayoutSizing_portraitCanvas_usesTallerOrbit() {
        val sizing = LivingTreeLayout.computeLayoutSizing(
            canvasWidth = 360f,
            canvasHeight = 640f,
            peopleCount = 8,
        )
        assertTrue(sizing.orbitRadiusY > sizing.orbitRadiusX)
        assertNoOverlap(
            count = 8,
            sizing = sizing,
            canvasWidth = 360f,
            canvasHeight = 640f,
        )
    }

    @Test
    fun radialPositions_ignoresPersistedAngles_andEvenlySpaces() {
        val ids = List(7) { index -> (index + 1).toLong() }
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, ids.size)

        val positions = LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = canvas / 2f,
            centerY = canvas / 2f,
            orbitRadiusX = sizing.orbitRadiusX,
            orbitRadiusY = sizing.orbitRadiusY,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
            storedPositions = ids.associateWith {
                LivingTreeLayout.StoredPosition(angleRadians = 0.0, radiusFraction = 0.35)
            },
        )

        assertEquals(7, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(7, distinct.size)
        assertNoOverlap(ids.size, sizing, positions)
    }

    @Test
    fun radialPositions_filteredSubset_evenlySpacesVisiblePeople() {
        val visibleIds = listOf(1L, 3L, 5L, 7L)
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, visibleIds.size)

        val positions = LivingTreeLayout.radialPositions(
            personIds = visibleIds,
            centerX = 50f,
            centerY = 50f,
            orbitRadiusX = sizing.orbitRadiusX,
            orbitRadiusY = sizing.orbitRadiusY,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )

        assertEquals(4, positions.size)
        val distinct = positions.map { Pair(it.x.toInt(), it.y.toInt()) }.toSet()
        assertEquals(4, distinct.size)
        assertNoOverlap(visibleIds.size, sizing, positions, canvasDim = canvas)
    }

    @Test
    fun storedPositionsFromPeople_returnsEmptyMap() {
        val stored = LivingTreeLayout.storedPositionsFromPeople(
            listOf(
                Triple(1L, 1.5, 0.8),
                Triple(2L, null, null),
            ),
        )
        assertTrue(stored.isEmpty())
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
    fun planRings_usesMultipleRingsBeforeShrinkingNodes() {
        val sizing = LivingTreeLayout.computeLayoutSizing(canvas, 24)
        val rings = LivingTreeLayout.planRings(
            totalCount = 24,
            orbitRadiusX = sizing.orbitRadiusX,
            orbitRadiusY = sizing.orbitRadiusY,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )

        assertTrue(rings.sumOf { it.count } >= 24)
        if (rings.size > 1) {
            assertTrue(rings.first().radiusFraction > rings.last().radiusFraction)
        }
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
    fun bubbleGap_isTenPixels() {
        assertEquals(10f, LivingTreeLayout.BUBBLE_GAP, 0.001f)
    }

    @Test
    fun computeLayoutSizing_fewerVisiblePeople_largerBubbles() {
        val manySizing = LivingTreeLayout.computeLayoutSizing(canvas, 40)
        val fewSizing = LivingTreeLayout.computeLayoutSizing(canvas, 4)
        assertTrue(fewSizing.nodeRadius > manySizing.nodeRadius)
    }

    @Test
    fun maxNodeRadiusForMultiRingLayout_usesMultipleRingsForLargeGroups() {
        val sizing = LivingTreeLayout.computeLayoutSizing(1080f, 24)
        assertNoOverlap(24, sizing, canvasDim = 1080f)
        val rings = LivingTreeLayout.planRings(
            totalCount = 24,
            orbitRadiusX = sizing.orbitRadiusX,
            orbitRadiusY = sizing.orbitRadiusY,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )
        assertTrue(rings.sumOf { it.count } >= 24)
        assertTrue(rings.size >= 2)
    }

    @Test
    fun circleOverlapsSpoke_detectsPipeCollision() {
        val centerRadius = 40f
        val nodeRadius = 24f
        val centerX = 100f
        val centerY = 100f
        val nodeX = 180f
        val nodeY = 100f
        val onPipeY = centerY + 18f
        val onPipeX = 120f
        assertTrue(
            LivingTreeLayout.circleOverlapsSpoke(
                circleX = onPipeX,
                circleY = onPipeY,
                circleRadius = nodeRadius,
                centerX = centerX,
                centerY = centerY,
                centerRadius = centerRadius,
                nodeX = nodeX,
                nodeY = nodeY,
                nodeRadius = nodeRadius,
            ),
        )
        assertFalse(
            LivingTreeLayout.circleOverlapsSpoke(
                circleX = centerX,
                circleY = centerY - 120f,
                circleRadius = nodeRadius,
                centerX = centerX,
                centerY = centerY,
                centerRadius = centerRadius,
                nodeX = nodeX,
                nodeY = nodeY,
                nodeRadius = nodeRadius,
            ),
        )
    }

    @Test
    fun computeLayoutSizing_midCounts_preferLargerBubblesWithoutSpokes() {
        // Without spoke clearance, mid-size groups stay large on a phone-sized canvas.
        val phoneCanvas = 1080f
        val sizing = LivingTreeLayout.computeLayoutSizing(phoneCanvas, 12)
        assertTrue(
            "Expected nodeRadius >= 56 for 12 people on phone canvas, got ${sizing.nodeRadius}",
            sizing.nodeRadius >= 56f,
        )
        assertNoOverlap(12, sizing, canvasDim = phoneCanvas)
        val tinySizing = LivingTreeLayout.computeLayoutSizing(canvas, 12)
        assertTrue(tinySizing.nodeRadius >= LivingTreeLayout.MIN_NODE_RADIUS)
        assertNoOverlap(12, tinySizing)
    }

    @Test
    fun planRings_spacesRingsByBubbleDiameter() {
        val orbit = 200f
        val nodeRadius = 30f
        val centerRadius = 40f
        val rings = LivingTreeLayout.planRings(
            totalCount = 20,
            orbitRadiusX = orbit,
            orbitRadiusY = orbit,
            nodeRadius = nodeRadius,
            centerRadius = centerRadius,
        )
        assertTrue(rings.size >= 2)
        val outer = rings[0].radiusFraction * orbit
        val inner = rings[1].radiusFraction * orbit
        assertTrue(outer - inner >= 2f * nodeRadius + LivingTreeLayout.BUBBLE_GAP - 0.5f)
    }

    private fun assertNoOverlap(
        count: Int,
        sizing: LivingTreeLayout.LayoutSizing,
        positions: List<LivingTreeLayout.NodePosition>? = null,
        canvasDim: Float = canvas,
        canvasWidth: Float = canvasDim,
        canvasHeight: Float = canvasDim,
    ) {
        val ids = (1L..count.toLong()).toList()
        val nodes = positions ?: LivingTreeLayout.radialPositions(
            personIds = ids,
            centerX = canvasWidth / 2f,
            centerY = canvasHeight / 2f,
            orbitRadiusX = sizing.orbitRadiusX,
            orbitRadiusY = sizing.orbitRadiusY,
            nodeRadius = sizing.nodeRadius,
            centerRadius = sizing.centerRadius,
        )
        assertEquals(count, nodes.size)
        val minSeparation = 2f * sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP - 1f
        val centerSeparation = sizing.centerRadius + sizing.nodeRadius + LivingTreeLayout.BUBBLE_GAP - 1f
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
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
