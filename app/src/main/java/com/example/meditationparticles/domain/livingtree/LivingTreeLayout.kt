package com.example.meditationparticles.domain.livingtree

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object LivingTreeLayout {
    const val MIN_NODE_RADIUS = 11f
    const val MAX_NODE_RADIUS = 44f
    const val MIN_CENTER_RADIUS = 30f
    const val MAX_CENTER_RADIUS = 60f
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
        val maxOrbit = maxOrbitRadius(minDimension, nodeRadius)
        val spacingLimitedNode = maxNodeRadiusForOrbit(maxOrbit, personCount)
        if (spacingLimitedNode < nodeRadius) {
            nodeRadius = spacingLimitedNode.coerceAtLeast(MIN_NODE_RADIUS)
        }

        val orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius)
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

    /**
     * Satellite bubble radius scales with canvas size and person count.
     * Few people → larger bubbles; many people → smaller (down to [MIN_NODE_RADIUS]).
     */
    fun computeNodeRadius(minDimension: Float, personCount: Int): Float {
        if (personCount <= 0) return MAX_NODE_RADIUS
        val countScale = countScale(personCount)
        val canvasScale = (minDimension / 400f).coerceIn(0.88f, 1.12f)
        val raw = minDimension * 0.11f * countScale * canvasScale
        return raw.coerceIn(MIN_NODE_RADIUS, MAX_NODE_RADIUS)
    }

    fun computeCenterRadius(minDimension: Float, nodeRadius: Float): Float {
        val fromNode = nodeRadius * CENTER_TO_NODE_RATIO
        val fromCanvas = minDimension * 0.095f
        return maxOf(fromNode, fromCanvas).coerceIn(
            maxOf(MIN_CENTER_RADIUS, nodeRadius * 1.2f),
            minOf(MAX_CENTER_RADIUS, minDimension * 0.14f),
        )
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

    internal fun countScale(personCount: Int): Float = when {
        personCount <= 3 -> 1.0f
        personCount <= 8 -> 0.88f
        personCount <= 15 -> 0.74f
        personCount <= 25 -> 0.60f
        personCount <= 40 -> 0.48f
        else -> 0.38f
    }

    internal fun orbitUtilization(personCount: Int): Float = when {
        personCount <= 4 -> 0.92f
        personCount <= 10 -> 0.88f
        personCount <= 20 -> 0.84f
        personCount <= 35 -> 0.80f
        else -> 0.76f
    }
}
