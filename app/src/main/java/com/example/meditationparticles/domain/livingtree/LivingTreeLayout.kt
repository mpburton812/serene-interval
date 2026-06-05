package com.example.meditationparticles.domain.livingtree

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

object LivingTreeLayout {
    const val MIN_NODE_RADIUS = 18f
    const val MAX_NODE_RADIUS = 156f
    const val MIN_CENTER_RADIUS = 56f
    const val MAX_CENTER_RADIUS = 216f
    const val CENTER_TO_NODE_RATIO = 1.45f
    const val CANVAS_PADDING = 24f
    const val BUBBLE_GAP = 8f
    const val ANGLE_EPSILON = 0.05
    const val MIN_RADIUS_FRACTION = 0.35
    const val MAX_RADIUS_FRACTION = 1.0

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
        storedPositions: Map<Long, StoredPosition> = emptyMap(),
    ): List<NodePosition> {
        if (personIds.isEmpty()) return emptyList()
        val resolved = resolveStoredPositions(personIds, storedPositions)
        return personIds.map { id ->
            val position = resolved.getValue(id)
            val effectiveRadius = orbitRadius * position.radiusFraction.toFloat()
            NodePosition(
                id = id,
                x = centerX + effectiveRadius * cos(position.angleRadians).toFloat(),
                y = centerY + effectiveRadius * sin(position.angleRadians).toFloat(),
                radius = nodeRadius,
            )
        }
    }

    /**
     * Persisted angle/radius values with nulls omitted. Layout resolves duplicates and nulls
     * via [resolveStoredPositions].
     */
    fun storedPositionsFromPeople(
        people: List<Triple<Long, Double?, Double?>>,
    ): Map<Long, StoredPosition> =
        people.mapNotNull { (id, angle, radiusFraction) ->
            angle?.let { id to StoredPosition(it, radiusFraction ?: 1.0) }
        }.toMap()

    /**
     * Evenly spaces people without a unique persisted angle. Duplicate angles (within
     * [ANGLE_EPSILON]) are treated as unset so stacked legacy rows re-spread on load.
     */
    fun resolveStoredPositions(
        personIds: List<Long>,
        stored: Map<Long, StoredPosition>,
    ): Map<Long, StoredPosition> {
        if (personIds.isEmpty()) return emptyMap()

        val duplicateIds = findDuplicateAngleIds(personIds, stored)
        val needsAuto = personIds.filter { id ->
            stored[id] == null || id in duplicateIds
        }
        val resolved = mutableMapOf<Long, StoredPosition>()
        personIds.forEach { id ->
            val storedPosition = stored[id]
            if (storedPosition != null && id !in duplicateIds) {
                resolved[id] = storedPosition.copy(
                    radiusFraction = storedPosition.radiusFraction.coerceIn(
                        MIN_RADIUS_FRACTION,
                        MAX_RADIUS_FRACTION,
                    ),
                )
            }
        }

        val autoCount = needsAuto.size
        if (autoCount > 0) {
            needsAuto.forEachIndexed { index, id ->
                resolved[id] = StoredPosition(
                    angleRadians = index * 2.0 * PI / autoCount,
                    radiusFraction = stored[id]?.radiusFraction?.coerceIn(
                        MIN_RADIUS_FRACTION,
                        MAX_RADIUS_FRACTION,
                    ) ?: 1.0,
                )
            }
        }
        return resolved
    }

    fun positionFromCanvasPoint(
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        x: Float,
        y: Float,
    ): StoredPosition {
        if (orbitRadius <= 0f) {
            return StoredPosition(angleRadians = 0.0, radiusFraction = 1.0)
        }
        val dx = (x - centerX).toDouble()
        val dy = (y - centerY).toDouble()
        val angle = kotlin.math.atan2(dy, dx)
        val radiusFraction = (hypot(dx, dy) / orbitRadius)
            .coerceIn(MIN_RADIUS_FRACTION.toDouble(), MAX_RADIUS_FRACTION.toDouble())
        return StoredPosition(angleRadians = angle, radiusFraction = radiusFraction)
    }

    internal fun findDuplicateAngleIds(
        personIds: List<Long>,
        stored: Map<Long, StoredPosition>,
    ): Set<Long> {
        val withAngles = personIds.mapNotNull { id ->
            stored[id]?.angleRadians?.let { id to normalizeAngle(it) }
        }
        val duplicateIds = mutableSetOf<Long>()
        for (i in withAngles.indices) {
            for (j in i + 1 until withAngles.size) {
                val (idA, angleA) = withAngles[i]
                val (idB, angleB) = withAngles[j]
                if (anglesNear(angleA, angleB)) {
                    duplicateIds += idA
                    duplicateIds += idB
                }
            }
        }
        return duplicateIds
    }

    internal fun normalizeAngle(angle: Double): Double {
        val twoPi = 2.0 * PI
        var normalized = angle % twoPi
        if (normalized < 0) normalized += twoPi
        return normalized
    }

    internal fun anglesNear(a: Double, b: Double): Boolean {
        val diff = abs(normalizeAngle(a) - normalizeAngle(b))
        return diff < ANGLE_EPSILON || diff > 2.0 * PI - ANGLE_EPSILON
    }

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

    fun angleForNewPerson(existingCount: Int): Double? = null

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
