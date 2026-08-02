package com.safehaven.affirmations.domain.livingtree

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
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
    const val PIPE_WIDTH_BASE = 12.5f
    const val PIPE_SCALE_REFERENCE = 80f

    data class LayoutSizing(
        val nodeRadius: Float,
        val orbitRadiusX: Float,
        val orbitRadiusY: Float,
        val centerRadius: Float,
    ) {
        /** Minor-axis orbit; useful for capacity checks and square-canvas callers. */
        val orbitRadius: Float get() = min(orbitRadiusX, orbitRadiusY)
    }

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
     * Square-canvas convenience overload.
     */
    fun computeLayoutSizing(minDimension: Float, peopleCount: Int): LayoutSizing =
        computeLayoutSizing(minDimension, minDimension, peopleCount)

    /**
     * Sizes spheres from [peopleCount] for a rectangular canvas. Prefers large bubbles packed
     * across elliptical rings; shrinks only when spheres cannot fit without overlap.
     */
    fun computeLayoutSizing(
        canvasWidth: Float,
        canvasHeight: Float,
        peopleCount: Int,
    ): LayoutSizing {
        val refDim = min(canvasWidth, canvasHeight)
        if (peopleCount <= 0) {
            val fallbackNode = MAX_NODE_RADIUS
            return LayoutSizing(
                nodeRadius = fallbackNode,
                orbitRadiusX = 0f,
                orbitRadiusY = 0f,
                centerRadius = effectiveCenterRadius(refDim, fallbackNode),
            )
        }

        val desiredNodeRadius = computeNodeRadius(refDim, peopleCount)
        val multiRingCap = if (peopleCount <= 1) {
            MAX_NODE_RADIUS
        } else {
            maxNodeRadiusForMultiRingLayout(canvasWidth, canvasHeight, peopleCount)
        }
        // Prefer multi-ring packing — do not cap by single-ring angular spacing.
        val baseNodeRadius = minOf(desiredNodeRadius, multiRingCap, MAX_NODE_RADIUS)
            .coerceIn(absoluteMinNodeRadius, MAX_NODE_RADIUS)

        var lowScale = (absoluteMinNodeRadius / baseNodeRadius).coerceIn(0.01f, 1f)
        var highScale = 1f
        repeat(24) {
            val mid = (lowScale + highScale) / 2f
            val scaledNode = baseNodeRadius * mid
            val scaledCenter = effectiveCenterRadius(refDim, scaledNode)
            if (layoutFitsWithoutOverlap(canvasWidth, canvasHeight, peopleCount, scaledNode, scaledCenter)) {
                lowScale = mid
            } else {
                highScale = mid
            }
        }

        val nodeRadius = (baseNodeRadius * lowScale).coerceAtLeast(absoluteMinNodeRadius)
        val centerRadius = effectiveCenterRadius(refDim, nodeRadius)
        val (orbitX, orbitY) = computeOrbitRadii(
            canvasWidth,
            canvasHeight,
            peopleCount,
            nodeRadius,
            centerRadius,
        )
        return LayoutSizing(nodeRadius, orbitX, orbitY, centerRadius)
    }

    /**
     * Finds the largest node radius that fits [peopleCount] across multiple rings without
     * sphere overlap.
     */
    fun maxNodeRadiusForMultiRingLayout(minDimension: Float, peopleCount: Int): Float =
        maxNodeRadiusForMultiRingLayout(minDimension, minDimension, peopleCount)

    fun maxNodeRadiusForMultiRingLayout(
        canvasWidth: Float,
        canvasHeight: Float,
        peopleCount: Int,
    ): Float {
        val refDim = min(canvasWidth, canvasHeight)
        if (peopleCount <= 0) return MAX_NODE_RADIUS
        if (peopleCount == 1) {
            val maxOrbitX = maxOrbitRadius(canvasWidth, MAX_NODE_RADIUS)
            val maxOrbitY = maxOrbitRadius(canvasHeight, MAX_NODE_RADIUS)
            val center = effectiveCenterRadius(refDim, MAX_NODE_RADIUS)
            val maxSingle = minOf(
                MAX_NODE_RADIUS,
                min(maxOrbitX, maxOrbitY) - center - BUBBLE_GAP,
            )
            return maxSingle.coerceAtLeast(absoluteMinNodeRadius)
        }

        var low = absoluteMinNodeRadius
        var high = minOf(computeNodeRadius(refDim, peopleCount), MAX_NODE_RADIUS)
        repeat(28) {
            val mid = (low + high) / 2f
            val center = effectiveCenterRadius(refDim, mid)
            if (layoutFitsWithoutOverlap(canvasWidth, canvasHeight, peopleCount, mid, center)) {
                low = mid
            } else {
                high = mid
            }
        }
        return low
    }

    /**
     * Evenly spaces [personIds] on one or more elliptical rings. Ignores persisted angles —
     * re-packed whenever the visible set changes (e.g. tag filter).
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
    ): List<NodePosition> = radialPositions(
        personIds = personIds,
        centerX = centerX,
        centerY = centerY,
        orbitRadiusX = orbitRadius,
        orbitRadiusY = orbitRadius,
        nodeRadius = nodeRadius,
        centerRadius = centerRadius,
        storedPositions = storedPositions,
        userPlacedIds = userPlacedIds,
    )

    fun radialPositions(
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadiusX: Float,
        orbitRadiusY: Float,
        nodeRadius: Float,
        centerRadius: Float = 0f,
        storedPositions: Map<Long, StoredPosition> = emptyMap(),
        userPlacedIds: Set<Long> = emptySet(),
    ): List<NodePosition> {
        if (personIds.isEmpty() || orbitRadiusX <= 0f || orbitRadiusY <= 0f) return emptyList()

        val rings = planRings(personIds.size, orbitRadiusX, orbitRadiusY, nodeRadius, centerRadius)
        val positions = mutableListOf<NodePosition>()
        var personIndex = 0
        rings.forEachIndexed { ringIndex, ring ->
            val ringOrbitX = orbitRadiusX * ring.radiusFraction.toFloat()
            val ringOrbitY = orbitRadiusY * ring.radiusFraction.toFloat()
            val slotAngle = 2.0 * PI / ring.count
            val angleOffset = if (ringIndex % 2 == 1) slotAngle / 2.0 else 0.0
            for (slot in 0 until ring.count) {
                val angle = angleOffset + slot * slotAngle
                val id = personIds[personIndex++]
                positions += NodePosition(
                    id = id,
                    x = centerX + ringOrbitX * cos(angle).toFloat(),
                    y = centerY + ringOrbitY * sin(angle).toFloat(),
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

    fun effectiveCenterRadius(minDimension: Float, nodeRadius: Float): Float =
        computeCenterRadius(minDimension, nodeRadius)
            .coerceAtLeast(nodeRadius * CENTER_TO_NODE_RATIO)

    fun computeOrbitRadius(
        minDimension: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float = computeCenterRadius(minDimension, nodeRadius),
    ): Float {
        val (orbitX, orbitY) = computeOrbitRadii(
            minDimension,
            minDimension,
            personCount,
            nodeRadius,
            centerRadius,
        )
        return min(orbitX, orbitY)
    }

    fun computeOrbitRadii(
        canvasWidth: Float,
        canvasHeight: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float,
    ): Pair<Float, Float> {
        if (personCount == 0) return 0f to 0f
        val maxOrbitX = maxOrbitRadius(canvasWidth, nodeRadius)
        val maxOrbitY = maxOrbitRadius(canvasHeight, nodeRadius)
        val minForCenter = minOrbitRadiusForCenterClearance(nodeRadius, centerRadius)
        if (personCount == 1) {
            val preferredX = minOf(maxOrbitX * 0.75f, maxOrbitX).coerceAtLeast(minForCenter)
            val preferredY = minOf(maxOrbitY * 0.75f, maxOrbitY).coerceAtLeast(minForCenter)
            return preferredX to preferredY
        }

        val utilization = orbitUtilization(personCount)
        val preferredX = minOf(maxOrbitX, maxOf(minForCenter, maxOrbitX * utilization))
        val preferredY = minOf(maxOrbitY, maxOf(minForCenter, maxOrbitY * utilization))
        return preferredX to preferredY
    }

    fun planRings(
        totalCount: Int,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): List<RingPlan> = planRings(totalCount, orbitRadius, orbitRadius, nodeRadius, centerRadius)

    /**
     * Packs people from the outer orbit inward. Ring radii are spaced by at least
     * `2 * nodeRadius + BUBBLE_GAP` on the minor axis so adjacent rings do not force
     * premature shrinking.
     */
    fun planRings(
        totalCount: Int,
        orbitRadiusX: Float,
        orbitRadiusY: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): List<RingPlan> {
        if (totalCount <= 0) return emptyList()
        val outerPacking = min(orbitRadiusX, orbitRadiusY)
        if (outerPacking <= 0f) return emptyList()

        val minPacking = centerRadius + nodeRadius + BUBBLE_GAP
        val ringGap = 2f * nodeRadius + BUBBLE_GAP
        val packingRadii = mutableListOf<Float>()
        var packing = outerPacking
        while (packing >= minPacking - 0.5f && packingRadii.size < MAX_RING_COUNT) {
            packingRadii += packing
            packing -= ringGap
        }
        if (packingRadii.isEmpty()) return emptyList()

        var remaining = totalCount
        val rings = mutableListOf<RingPlan>()
        for (ringPacking in packingRadii) {
            if (remaining <= 0) break
            val scale = ringPacking / outerPacking
            val ringOrbitX = orbitRadiusX * scale
            val ringOrbitY = orbitRadiusY * scale
            val capacity = maxBubblesOnRing(ringOrbitX, ringOrbitY, nodeRadius, centerRadius)
            if (capacity <= 0) continue
            val take = minOf(remaining, capacity)
            rings += RingPlan(
                radiusFraction = (ringPacking / outerPacking).toDouble(),
                count = take,
            )
            remaining -= take
        }
        return rings
    }

    fun maxBubblesOnRing(ringOrbit: Float, nodeRadius: Float, centerRadius: Float): Int =
        maxBubblesOnRing(ringOrbit, ringOrbit, nodeRadius, centerRadius)

    /**
     * Capacity for equal angular spacing on an ellipse, using the minor axis so adjacent
     * bubbles stay clear even at the tightest parametric spacing.
     */
    fun maxBubblesOnRing(
        ringOrbitX: Float,
        ringOrbitY: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): Int {
        val packingRadius = min(ringOrbitX, ringOrbitY)
        if (packingRadius <= 0f) return 0
        if (packingRadius < centerRadius + nodeRadius + BUBBLE_GAP - 0.5f) return 0
        val halfAngle = asin(((nodeRadius + BUBBLE_GAP / 2f) / packingRadius).coerceIn(0f, 1f))
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

    fun pipeHalfWidth(centerRadius: Float): Float =
        (centerRadius / PIPE_SCALE_REFERENCE * PIPE_WIDTH_BASE / 2f).coerceIn(5f, 10f)

    fun pointToSegmentDistance(
        px: Float,
        py: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 0.001f) {
            return hypot((px - x1).toDouble(), (py - y1).toDouble()).toFloat()
        }
        val t = ((px - x1) * dx + (py - y1) * dy) / lengthSq
        val clamped = t.coerceIn(0f, 1f)
        val projX = x1 + clamped * dx
        val projY = y1 + clamped * dy
        return hypot((px - projX).toDouble(), (py - projY).toDouble()).toFloat()
    }

    fun circleOverlapsSpoke(
        circleX: Float,
        circleY: Float,
        circleRadius: Float,
        centerX: Float,
        centerY: Float,
        centerRadius: Float,
        nodeX: Float,
        nodeY: Float,
        nodeRadius: Float,
        gap: Float = BUBBLE_GAP,
    ): Boolean {
        if (!bearingsDiffer(centerX, centerY, circleX, circleY, nodeX, nodeY)) {
            return false
        }
        val dx = nodeX - centerX
        val dy = nodeY - centerY
        val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (length < 1f) return false
        val nx = dx / length
        val ny = dy / length
        val fromX = centerX + nx * centerRadius
        val fromY = centerY + ny * centerRadius
        val toX = nodeX - nx * nodeRadius
        val toY = nodeY - ny * nodeRadius
        val clearance = circleRadius + pipeHalfWidth(centerRadius) + gap
        return pointToSegmentDistance(circleX, circleY, fromX, fromY, toX, toY) < clearance
    }

    fun bearingsDiffer(
        centerX: Float,
        centerY: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        minRadians: Double = 0.28,
    ): Boolean = angularDistance(
        atan2((ay - centerY).toDouble(), (ax - centerX).toDouble()),
        atan2((by - centerY).toDouble(), (bx - centerX).toDouble()),
    ) > minRadians

    fun angularDistance(a: Double, b: Double): Double {
        var delta = kotlin.math.abs(a - b) % (2.0 * PI)
        if (delta > PI) delta = 2.0 * PI - delta
        return delta
    }

    internal const val absoluteMinNodeRadius = 11f

    /** Softer mid-count penalties so layout prefers larger bubbles and fills rings first. */
    internal fun countScale(personCount: Int): Float = when {
        personCount <= 5 -> 1.0f
        personCount <= 10 -> 0.94f
        personCount <= 20 -> 0.82f
        personCount <= 30 -> 0.68f
        personCount <= 45 -> 0.52f
        else -> 0.40f
    }

    internal fun orbitUtilization(personCount: Int): Float = when {
        personCount <= 4 -> 0.96f
        personCount <= 10 -> 0.94f
        personCount <= 20 -> 0.90f
        personCount <= 35 -> 0.86f
        else -> 0.82f
    }

    internal fun layoutFitsWithoutOverlap(
        minDimension: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float = effectiveCenterRadius(minDimension, nodeRadius),
    ): Boolean = layoutFitsWithoutOverlap(
        canvasWidth = minDimension,
        canvasHeight = minDimension,
        personCount = personCount,
        nodeRadius = nodeRadius,
        centerRadius = centerRadius,
    )

    internal fun layoutFitsWithoutOverlap(
        canvasWidth: Float,
        canvasHeight: Float,
        personCount: Int,
        nodeRadius: Float,
        centerRadius: Float,
    ): Boolean {
        val refDim = min(canvasWidth, canvasHeight)
        if (nodeRadius < absoluteMinNodeRadius - 0.5f) return false
        if (centerRadius < nodeRadius * CENTER_TO_NODE_RATIO - 0.5f) return false
        val maxOrbitX = maxOrbitRadius(canvasWidth, nodeRadius)
        val maxOrbitY = maxOrbitRadius(canvasHeight, nodeRadius)
        val (orbitX, orbitY) = computeOrbitRadii(
            canvasWidth,
            canvasHeight,
            personCount,
            nodeRadius,
            centerRadius,
        )
        if (orbitX > maxOrbitX + 0.5f || orbitY > maxOrbitY + 0.5f) return false

        val ringCapacity = planRings(personCount, orbitX, orbitY, nodeRadius, centerRadius)
            .sumOf { it.count }
        if (ringCapacity < personCount) return false

        val ids = (1L..personCount.toLong()).toList()
        val positions = radialPositions(
            personIds = ids,
            centerX = canvasWidth / 2f,
            centerY = canvasHeight / 2f,
            orbitRadiusX = orbitX,
            orbitRadiusY = orbitY,
            nodeRadius = nodeRadius,
            centerRadius = centerRadius,
        )
        if (positions.size != personCount) return false
        return !positionsOverlap(
            positions = positions,
            centerX = canvasWidth / 2f,
            centerY = canvasHeight / 2f,
            centerRadius = centerRadius,
        )
    }

    /**
     * Sphere↔sphere and sphere↔center only. Spoke/pipe clearance is intentionally ignored
     * so multi-ring packing can keep larger bubbles.
     */
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
