package com.example.meditationparticles.domain.livingtree

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object LivingTreeLayout {
    const val MIN_NODE_RADIUS = 18f
    const val MAX_NODE_RADIUS = 156f
    const val MIN_CENTER_RADIUS = 56f
    const val MAX_CENTER_RADIUS = 216f
    const val CENTER_TO_NODE_RATIO = 1.45f
    const val CANVAS_PADDING = 24f
    const val BUBBLE_GAP = 8f

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

    fun computeLayoutSizing(minDimension: Float, personCount: Int): LayoutSizing {
        if (personCount <= 0) {
            val fallbackNode = MAX_NODE_RADIUS
            return LayoutSizing(
                nodeRadius = fallbackNode,
                orbitRadius = 0f,
                centerRadius = computeCenterRadius(minDimension, fallbackNode),
            )
        }

        var nodeRadius = computeNodeRadius(minDimension, personCount)
        val canvasOrbit = maxOrbitRadius(minDimension, absoluteMinNodeRadius)
        val spacingCap = maxNodeRadiusForOrbit(canvasOrbit, personCount)
        nodeRadius = minOf(nodeRadius, spacingCap)
        val countFloor = minNodeRadiusForCount(personCount)
        val effectiveFloor = minOf(countFloor, spacingCap).coerceAtLeast(
            minOf(absoluteMinNodeRadius, spacingCap),
        )
        nodeRadius = nodeRadius.coerceAtLeast(effectiveFloor)

        var orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius)
        if (personCount > 1 && nodeRadius > 1f) {
            var low = 1f
            var high = nodeRadius
            repeat(20) {
                val mid = (low + high) / 2f
                val trialOrbit = computeOrbitRadius(minDimension, personCount, mid)
                val chord = 2.0 * trialOrbit * sin(PI / personCount)
                val required = 2.0 * mid + BUBBLE_GAP
                if (chord >= required - 0.5) {
                    low = mid
                } else {
                    high = mid
                }
            }
            nodeRadius = low
            orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius)
        }
        val centerRadius = computeCenterRadius(minDimension, nodeRadius)
        return LayoutSizing(nodeRadius, orbitRadius, centerRadius)
    }

    fun radialPositions(
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
        storedAngles: Map<Long, Double> = emptyMap(),
    ): List<NodePosition> {
        if (personIds.isEmpty()) return emptyList()
        val count = personIds.size
        return personIds.mapIndexed { index, id ->
            val angle = storedAngles[id] ?: (index * 2.0 * PI / count)
            NodePosition(
                id = id,
                x = centerX + orbitRadius * cos(angle).toFloat(),
                y = centerY + orbitRadius * sin(angle).toFloat(),
                radius = nodeRadius,
            )
        }
    }

    /** Only include persisted angles; null DB values fall back to even spacing in [radialPositions]. */
    fun storedAnglesFromPeople(people: List<Pair<Long, Double?>>): Map<Long, Double> =
        people.mapNotNull { (id, angle) -> angle?.let { id to it } }.toMap()

    /**
     * Satellite bubble radius scales with canvas size and person count.
     * Few people → larger bubbles; many people → smaller (down to [MIN_NODE_RADIUS]).
     */
    fun computeNodeRadius(minDimension: Float, personCount: Int): Float {
        if (personCount <= 0) return MAX_NODE_RADIUS
        val countScale = countScale(personCount)
        val canvasScale = (minDimension / 400f).coerceIn(0.88f, 1.12f)
        val raw = minDimension * 0.375f * countScale * canvasScale
        return raw.coerceIn(MIN_NODE_RADIUS, MAX_NODE_RADIUS)
    }

    fun computeCenterRadius(minDimension: Float, nodeRadius: Float): Float {
        val fromNode = nodeRadius * CENTER_TO_NODE_RATIO
        val fromCanvas = minDimension * 0.095f
        val minBound = maxOf(MIN_CENTER_RADIUS, nodeRadius * 1.2f)
        val maxBound = maxOf(
            minBound,
            minOf(MAX_CENTER_RADIUS, maxOf(minDimension * 0.20f, fromNode * 1.05f)),
        )
        return maxOf(fromNode, fromCanvas).coerceIn(minBound, maxBound)
    }

    /**
     * Spread satellites across available canvas while respecting angular spacing.
     */
    fun computeOrbitRadius(minDimension: Float, personCount: Int, nodeRadius: Float): Float {
        if (personCount == 0) return 0f
        val maxOrbit = maxOrbitRadius(minDimension, nodeRadius)
        if (personCount == 1) return maxOrbit * 0.75f

        val minForSpacing = minOrbitRadiusForSpacing(personCount, nodeRadius)
        val utilization = orbitUtilization(personCount)
        val preferred = maxOrbit * utilization
        return minOf(maxOrbit, maxOf(minForSpacing, preferred))
    }

    fun minOrbitRadiusForSpacing(personCount: Int, nodeRadius: Float): Float {
        if (personCount <= 1) return 0f
        val halfAngle = sin(PI / personCount).toFloat().coerceAtLeast(0.001f)
        return (nodeRadius + BUBBLE_GAP / 2f) / halfAngle
    }

    fun maxNodeRadiusForOrbit(maxOrbit: Float, personCount: Int): Float {
        if (personCount <= 1) return MAX_NODE_RADIUS
        val halfAngle = sin(PI / personCount).toFloat()
        return maxOrbit * halfAngle - BUBBLE_GAP / 2f
    }

    fun maxOrbitRadius(minDimension: Float, nodeRadius: Float): Float =
        minDimension / 2f - nodeRadius - CANVAS_PADDING

    fun angularSpacingRadians(personCount: Int): Double =
        if (personCount <= 0) 0.0 else 2.0 * PI / personCount

    fun angleForNewPerson(existingCount: Int): Double =
        if (existingCount == 0) 0.0 else existingCount * 2.0 * PI / existingCount

    /** Absolute lower cap; count-aware floors in [minNodeRadiusForCount] may be higher when spacing allows. */
    internal const val absoluteMinNodeRadius = 11f

    internal fun minNodeRadiusForCount(personCount: Int): Float = when {
        personCount <= 5 -> 84f
        personCount <= 10 -> 72f
        personCount <= 20 -> 54f
        personCount <= 30 -> 36f
        else -> absoluteMinNodeRadius
    }

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
}
