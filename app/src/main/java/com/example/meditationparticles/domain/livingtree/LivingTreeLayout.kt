package com.example.meditationparticles.domain.livingtree

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object LivingTreeLayout {
    const val MIN_NODE_RADIUS = 18f
    const val MAX_NODE_RADIUS = 156f
    const val MIN_CENTER_RADIUS = 56f
    const val MAX_CENTER_RADIUS = 216f
    const val CENTER_TO_NODE_RATIO = 1.2f
    const val CANVAS_PADDING = 24f
    const val BUBBLE_GAP = 10f
    const val MIN_RADIUS_FRACTION = 0.35
    const val MAX_RADIUS_FRACTION = 1.0
    const val DEFAULT_SIZE_MULTIPLIER = 3f
    const val MAX_RING_COUNT = 6

    data class LayoutSizing(
        val nodeRadius: Float,
        val orbitRadius: Float,
        val centerRadius: Float,
    )

    data class NodePosition(
        val id: Long,
        val x: Float,
        val y: Float,
        val radius: Float,
    )

    data class StoredPosition(
        val angleRadians: Double,
        val radiusFraction: Double = 1.0,
    )

    data class RingPlan(
        val radiusFraction: Double,
        val count: Int,
    )

    /**
     * Sizes spheres from [peopleCount]. Uses multi-ring placement before shrinking bubbles.
     */
    fun computeLayoutSizing(minDimension: Float, peopleCount: Int): LayoutSizing {
        if (peopleCount <= 0) {
            val fallbackNode = MAX_NODE_RADIUS
            return LayoutSizing(
                nodeRadius = fallbackNode,
                orbitRadius = 0f,
                centerRadius = computeCenterRadius(minDimension, fallbackNode),
            )
        }

        val desiredNodeRadius = computeNodeRadius(minDimension, peopleCount)
        val canvasOrbit = maxOrbitRadius(minDimension, absoluteMinNodeRadius).coerceAtLeast(1f)
        val spacingCap = maxNodeRadiusForOrbit(canvasOrbit, peopleCount)
            .coerceIn(absoluteMinNodeRadius, MAX_NODE_RADIUS)
        val baseNodeRadius = minOf(desiredNodeRadius, spacingCap, MAX_NODE_RADIUS)
            .coerceIn(absoluteMinNodeRadius, MAX_NODE_RADIUS)
        val baseCenterRadius = computeCenterRadius(minDimension, baseNodeRadius)

        var lowScale = (absoluteMinNodeRadius / baseNodeRadius).coerceIn(0.01f, 1f)
        var highScale = 1f
        repeat(24) {
            val mid = (lowScale + highScale) / 2f
            val scaledNode = baseNodeRadius * mid
            val scaledCenter = (baseCenterRadius * mid).coerceAtLeast(scaledNode * CENTER_TO_NODE_RATIO)
            if (layoutFitsWithoutOverlap(minDimension, peopleCount, scaledNode, scaledCenter)) {
                lowScale = mid
            } else {
                highScale = mid
            }
        }

        val nodeRadius = (baseNodeRadius * lowScale).coerceAtLeast(absoluteMinNodeRadius)
        val centerRadius = (baseCenterRadius * lowScale).coerceAtLeast(nodeRadius * CENTER_TO_NODE_RATIO)
        val orbitRadius = computeOrbitRadius(minDimension, peopleCount, nodeRadius, centerRadius)
        return LayoutSizing(nodeRadius, orbitRadius, centerRadius)
    }

    /**
     * Evenly spaces [personIds] on one or more rings. Ignores persisted angles — re-packed whenever
     * the visible set changes (e.g. tag filter).
     */
    fun radialPositions(
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float = 0f,
        storedPositions: Map<Long, StoredPosition> = emptyMap(),
        userPlacedIds: Set<Long> = emptySet(),
    ): List<NodePosition> {
        if (personIds.isEmpty() || orbitRadius <= 0f) return emptyList()

        val rings = planRings(personIds.size, orbitRadius, nodeRadius, centerRadius)
        val positions = mutableListOf<NodePosition>()
        var personIndex = 0
        rings.forEachIndexed { ringIndex, ring ->
            val ringOrbit = orbitRadius * ring.radiusFraction.toFloat()
            val angleOffset = ringIndex * PI / personIds.size.coerceAtLeast(1)
            for (slot in 0 until ring.count) {
                val angle = angleOffset + slot * 2.0 * PI / ring.count
                val id = personIds[personIndex++]
                positions += NodePosition(
                    id = id,
                    x = centerX + ringOrbit * cos(angle).toFloat(),
                    y = centerY + ringOrbit * sin(angle).toFloat(),
                    radius = nodeRadius,
                )
            }
        }
        return positions
    }

    /** @deprecated Layout no longer reads persisted angles; kept for repository/import compatibility. */
    fun storedPositionsFromPeople(
        people: List<Triple<Long, Double?, Double?>>,
    ): Map<Long, StoredPosition> = emptyMap()

    fun angleForNewPerson(existingCount: Int): Double? = null

    fun computeNodeRadius(minDimension: Float, personCount: Int): Float {
        if (personCount <= 0) return MAX_NODE_RADIUS
        val countScale = countScale(personCount)
        val canvasScale = (minDimension / 400f).coerceIn(0.88f, 1.12f)
        val raw = minDimension * 0.375f * DEFAULT_SIZE_MULTIPLIER * countScale * canvasScale
        return raw.coerceIn(MIN_NODE_RADIUS, MAX_NODE_RADIUS)
    }

    fun computeCenterRadius(minDimension: Float, nodeRadius: Float): Float {
        val fromNode = nodeRadius * CENTER_TO_NODE_RATIO
        val fromCanvas = minDimension * 0.095f
        val minBound = nodeRadius * CENTER_TO_NODE_RATIO
        val maxBound = minOf(
            MAX_CENTER_RADIUS,
            maxOf(minBound, minDimension * 0.20f),
        )
        return maxOf(fromNode, fromCanvas).coerceIn(minBound, maxBound)
    }

    fun computeOrbitRadius(
        minDimension: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float = computeCenterRadius(minDimension, nodeRadius),
    ): Float {
        if (personCount == 0) return 0f
        val maxOrbit = maxOrbitRadius(minDimension, nodeRadius)
        if (personCount == 1) {
            val minForCenter = minOrbitRadiusForCenterClearance(nodeRadius, centerRadius)
            return minOf(maxOrbit * 0.75f, maxOrbit).coerceAtLeast(minForCenter)
        }

        val minForCenter = minOrbitRadiusForCenterClearance(nodeRadius, centerRadius)
        val utilization = orbitUtilization(personCount)
        val preferred = maxOrbit * utilization
        return minOf(maxOrbit, maxOf(minForCenter, preferred))
    }

    fun planRings(
        totalCount: Int,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): List<RingPlan> {
        if (totalCount <= 0) return emptyList()

        var remaining = totalCount
        val rings = mutableListOf<RingPlan>()
        for (fraction in ringFractions()) {
            if (remaining <= 0) break
            val ringOrbit = orbitRadius * fraction.toFloat()
            val capacity = maxBubblesOnRing(ringOrbit, nodeRadius, centerRadius)
            if (capacity <= 0) continue
            val take = minOf(remaining, capacity)
            rings += RingPlan(radiusFraction = fraction, count = take)
            remaining -= take
        }
        return rings
    }

    fun maxBubblesOnRing(ringOrbit: Float, nodeRadius: Float, centerRadius: Float): Int {
        if (ringOrbit <= 0f) return 0
        if (ringOrbit < centerRadius + nodeRadius + BUBBLE_GAP - 0.5f) return 0
        val halfAngle = asin(((nodeRadius + BUBBLE_GAP / 2f) / ringOrbit).coerceIn(0f, 1f))
        if (halfAngle <= 0f) return 0
        return (PI / halfAngle).toInt().coerceAtLeast(1)
    }

    fun ringFractions(): List<Double> {
        if (MAX_RING_COUNT <= 1) return listOf(MAX_RADIUS_FRACTION)
        val step = (MAX_RADIUS_FRACTION - MIN_RADIUS_FRACTION) / (MAX_RING_COUNT - 1)
        return (0 until MAX_RING_COUNT).map { index ->
            MAX_RADIUS_FRACTION - index * step
        }
    }

    fun minOrbitRadiusForCenterClearance(nodeRadius: Float, centerRadius: Float): Float =
        centerRadius + nodeRadius + BUBBLE_GAP

    fun minOrbitRadiusForSpacing(personCount: Int, nodeRadius: Float): Float {
        if (personCount <= 1) return 0f
        val halfAngle = sin(PI / personCount).toFloat().coerceAtLeast(0.001f)
        return (nodeRadius + BUBBLE_GAP / 2f) / halfAngle
    }

    fun maxNodeRadiusForOrbit(maxOrbit: Float, personCount: Int): Float {
        if (personCount <= 1) return MAX_NODE_RADIUS
        if (maxOrbit <= 0f) return absoluteMinNodeRadius
        val halfAngle = sin(PI / personCount).toFloat()
        return (maxOrbit * halfAngle - BUBBLE_GAP / 2f).coerceAtLeast(absoluteMinNodeRadius)
    }

    fun maxOrbitRadius(minDimension: Float, nodeRadius: Float): Float =
        minDimension / 2f - nodeRadius - CANVAS_PADDING

    fun angularSpacingRadians(personCount: Int): Double =
        if (personCount <= 0) 0.0 else 2.0 * PI / personCount

    fun minCenterDistance(
        x1: Float,
        y1: Float,
        r1: Float,
        x2: Float,
        y2: Float,
        r2: Float,
    ): Float = hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toFloat() - (r1 + r2)

    fun circlesOverlap(
        x1: Float,
        y1: Float,
        r1: Float,
        x2: Float,
        y2: Float,
        r2: Float,
        gap: Float = BUBBLE_GAP,
    ): Boolean = minCenterDistance(x1, y1, r1, x2, y2, r2) < gap

    internal const val absoluteMinNodeRadius = 11f

    internal fun countScale(personCount: Int): Float = when {
        personCount <= 5 -> 1.0f
        personCount <= 10 -> 0.88f
        personCount <= 20 -> 0.72f
        personCount <= 30 -> 0.58f
        personCount <= 45 -> 0.48f
        else -> 0.40f
    }

    internal fun orbitUtilization(personCount: Int): Float = when {
        personCount <= 4 -> 0.92f
        personCount <= 10 -> 0.88f
        personCount <= 20 -> 0.84f
        personCount <= 35 -> 0.80f
        else -> 0.76f
    }

    internal fun layoutFitsWithoutOverlap(
        minDimension: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float = computeCenterRadius(minDimension, nodeRadius),
    ): Boolean {
        if (nodeRadius < absoluteMinNodeRadius - 0.5f) return false
        if (centerRadius < nodeRadius * CENTER_TO_NODE_RATIO - 0.5f) return false
        val maxOrbit = maxOrbitRadius(minDimension, nodeRadius)
        val orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius, centerRadius)
        if (orbitRadius > maxOrbit + 0.5f) return false

        val ringCapacity = planRings(personCount, orbitRadius, nodeRadius, centerRadius)
            .sumOf { it.count }
        if (ringCapacity < personCount) return false

        val ids = (1L..personCount.toLong()).toList()
        val positions = radialPositions(
            personIds = ids,
            centerX = minDimension / 2f,
            centerY = minDimension / 2f,
            orbitRadius = orbitRadius,
            nodeRadius = nodeRadius,
            centerRadius = centerRadius,
        )
        if (positions.size != personCount) return false
        return !positionsOverlap(
            positions = positions,
            centerX = minDimension / 2f,
            centerY = minDimension / 2f,
            centerRadius = centerRadius,
        )
    }

    internal fun positionsOverlap(
        positions: List<NodePosition>,
        centerX: Float,
        centerY: Float,
        centerRadius: Float,
    ): Boolean {
        for (i in positions.indices) {
            val a = positions[i]
            if (circlesOverlap(a.x, a.y, a.radius, centerX, centerY, centerRadius)) return true
            for (j in i + 1 until positions.size) {
                val b = positions[j]
                if (circlesOverlap(a.x, a.y, a.radius, b.x, b.y, b.radius)) return true
            }
        }
        return false
    }
}
