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
    const val DEFAULT_SIZE_MULTIPLIER = 3f

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

        val desiredNodeRadius = computeNodeRadius(minDimension, personCount)
        val canvasOrbit = maxOrbitRadius(minDimension, absoluteMinNodeRadius).coerceAtLeast(1f)
        val spacingCap = maxNodeRadiusForOrbit(canvasOrbit, personCount)
            .coerceIn(absoluteMinNodeRadius, MAX_NODE_RADIUS)
        val upperBound = minOf(desiredNodeRadius, spacingCap, MAX_NODE_RADIUS)
            .coerceIn(absoluteMinNodeRadius, MAX_NODE_RADIUS)

        var nodeRadius = upperBound
        if (upperBound > 1f + 0.5f) {
            var low = 1f
            var high = upperBound
            repeat(24) {
                val mid = (low + high) / 2f
                if (layoutFitsWithoutOverlap(minDimension, personCount, mid)) {
                    low = mid
                } else {
                    high = mid
                }
            }
            nodeRadius = if (personCount > 1) low else low.coerceAtMost(upperBound)
        }

        val centerRadius = computeCenterRadius(minDimension, nodeRadius)
        val orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius, centerRadius)
        return LayoutSizing(nodeRadius, orbitRadius, centerRadius)
    }

    fun radialPositions(
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float = 0f,
        storedPositions: Map<Long, StoredPosition> = emptyMap(),
    ): List<NodePosition> {
        if (personIds.isEmpty()) return emptyList()
        val resolved = resolveStoredPositions(
            personIds = personIds,
            stored = storedPositions,
            orbitRadius = orbitRadius,
            nodeRadius = nodeRadius,
            centerRadius = centerRadius,
        )
        val positions = personIds.map { id ->
            val position = resolved.getValue(id)
            nodePositionFromStored(
                id = id,
                stored = position,
                centerX = centerX,
                centerY = centerY,
                orbitRadius = orbitRadius,
                nodeRadius = nodeRadius,
            )
        }
        return resolvePositionOverlaps(
            positions = positions,
            centerX = centerX,
            centerY = centerY,
            centerRadius = centerRadius,
            orbitRadius = orbitRadius,
            nodeRadius = nodeRadius,
        )
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
     * [ANGLE_EPSILON]) and persisted layouts that overlap are re-spread on load.
     */
    fun resolveStoredPositions(
        personIds: List<Long>,
        stored: Map<Long, StoredPosition>,
        orbitRadius: Float = 0f,
        nodeRadius: Float = 0f,
        centerRadius: Float = 0f,
    ): Map<Long, StoredPosition> {
        if (personIds.isEmpty()) return emptyMap()

        val duplicateIds = findDuplicateAngleIds(personIds, stored)
        val overlapIds = if (orbitRadius > 0f && nodeRadius > 0f) {
            findOverlappingStoredIds(personIds, stored, orbitRadius, nodeRadius, centerRadius)
        } else {
            emptySet()
        }
        val needsAuto = personIds.filter { id ->
            stored[id] == null || id in duplicateIds || id in overlapIds
        }
        val resolved = mutableMapOf<Long, StoredPosition>()
        personIds.forEach { id ->
            val storedPosition = stored[id]
            if (storedPosition != null && id !in duplicateIds && id !in overlapIds) {
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

    /**
     * Adjusts a dragged position so it does not overlap the center bubble or any satellite.
     */
    fun nudgeStoredPosition(
        draggedId: Long,
        proposed: StoredPosition,
        otherStored: Map<Long, StoredPosition>,
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): StoredPosition {
        if (orbitRadius <= 0f || personIds.isEmpty()) return proposed

        var candidate = proposed.copy(
            radiusFraction = proposed.radiusFraction.coerceIn(
                MIN_RADIUS_FRACTION.toDouble(),
                MAX_RADIUS_FRACTION.toDouble(),
            ),
        )
        val others = personIds.filter { it != draggedId }.map { id ->
            nodePositionFromStored(
                id = id,
                stored = otherStored[id] ?: StoredPosition(angleRadians = 0.0),
                centerX = centerX,
                centerY = centerY,
                orbitRadius = orbitRadius,
                nodeRadius = nodeRadius,
            )
        }

        for (attempt in 0 until 48) {
            val dragged = nodePositionFromStored(
                id = draggedId,
                stored = candidate,
                centerX = centerX,
                centerY = centerY,
                orbitRadius = orbitRadius,
                nodeRadius = nodeRadius,
            )
            if (!hasOverlap(dragged, others, centerX, centerY, centerRadius)) {
                return candidate
            }

            val nudgedFraction = (candidate.radiusFraction + 0.025).coerceAtMost(MAX_RADIUS_FRACTION.toDouble())
            if (nudgedFraction > candidate.radiusFraction + 1e-6) {
                candidate = candidate.copy(radiusFraction = nudgedFraction)
                continue
            }

            val nudgedAngle = candidate.angleRadians + angularSpacingRadians(personIds.size) * 0.12
            candidate = candidate.copy(
                angleRadians = nudgedAngle,
                radiusFraction = MIN_RADIUS_FRACTION.toDouble(),
            )
        }
        return candidate
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
     * Few people → ~3× larger bubbles; many people → smaller (down to [MIN_NODE_RADIUS]).
     */
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
        val minBound = maxOf(MIN_CENTER_RADIUS, nodeRadius * 1.2f)
        val maxBound = maxOf(
            minBound,
            minOf(MAX_CENTER_RADIUS, maxOf(minDimension * 0.20f, fromNode * 1.05f)),
        )
        return maxOf(fromNode, fromCanvas).coerceIn(minBound, maxBound)
    }

    /**
     * Spread satellites across available canvas while respecting angular spacing and center clearance.
     */
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

        val minForSpacing = minOrbitRadiusForSpacing(personCount, nodeRadius)
        val minForCenter = minOrbitRadiusForCenterClearance(nodeRadius, centerRadius)
        val utilization = orbitUtilization(personCount)
        val preferred = maxOrbit * utilization
        return minOf(maxOrbit, maxOf(minForSpacing, minForCenter, preferred))
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

    fun angleForNewPerson(existingCount: Int): Double? = null

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

    internal fun layoutFitsWithoutOverlap(
        minDimension: Float,
        personCount: Int,
        nodeRadius: Float,
    ): Boolean {
        if (nodeRadius <= 0f) return false
        val centerRadius = computeCenterRadius(minDimension, nodeRadius)
        val maxOrbit = maxOrbitRadius(minDimension, nodeRadius)
        val orbitRadius = computeOrbitRadius(minDimension, personCount, nodeRadius, centerRadius)
        if (orbitRadius > maxOrbit + 0.5f) return false
        if (personCount <= 1) {
            return orbitRadius >= centerRadius + nodeRadius + BUBBLE_GAP - 0.5f
        }

        val chord = 2.0 * orbitRadius * sin(PI / personCount)
        val requiredSatellite = 2.0 * nodeRadius + BUBBLE_GAP
        if (chord < requiredSatellite - 0.5) return false

        return orbitRadius >= centerRadius + nodeRadius + BUBBLE_GAP - 0.5f
    }

    internal fun nodePositionFromStored(
        id: Long,
        stored: StoredPosition,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
    ): NodePosition {
        val effectiveRadius = orbitRadius * stored.radiusFraction.toFloat()
        return NodePosition(
            id = id,
            x = centerX + effectiveRadius * cos(stored.angleRadians).toFloat(),
            y = centerY + effectiveRadius * sin(stored.angleRadians).toFloat(),
            radius = nodeRadius,
        )
    }

    internal fun findOverlappingStoredIds(
        personIds: List<Long>,
        stored: Map<Long, StoredPosition>,
        orbitRadius: Float,
        nodeRadius: Float,
        centerRadius: Float,
    ): Set<Long> {
        val positions = personIds.mapNotNull { id ->
            stored[id]?.let { storedPosition ->
                nodePositionFromStored(
                    id = id,
                    stored = storedPosition,
                    centerX = 0f,
                    centerY = 0f,
                    orbitRadius = orbitRadius,
                    nodeRadius = nodeRadius,
                )
            }
        }
        val overlapping = mutableSetOf<Long>()
        for (i in positions.indices) {
            val a = positions[i]
            if (circlesOverlap(a.x, a.y, a.radius, 0f, 0f, centerRadius)) {
                overlapping += a.id
            }
            for (j in i + 1 until positions.size) {
                val b = positions[j]
                if (circlesOverlap(a.x, a.y, a.radius, b.x, b.y, b.radius)) {
                    overlapping += a.id
                    overlapping += b.id
                }
            }
        }
        return overlapping
    }

    internal fun resolvePositionOverlaps(
        positions: List<NodePosition>,
        centerX: Float,
        centerY: Float,
        centerRadius: Float,
        orbitRadius: Float,
        nodeRadius: Float,
    ): List<NodePosition> {
        if (positions.size <= 1 || orbitRadius <= 0f) return positions

        val adjusted = positions.toMutableList()
        for (index in adjusted.indices) {
            val position = adjusted[index]
            val angle = kotlin.math.atan2(
                (position.y - centerY).toDouble(),
                (position.x - centerX).toDouble(),
            )
            var fraction = (hypot(
                (position.x - centerX).toDouble(),
                (position.y - centerY).toDouble(),
            ) / orbitRadius).coerceIn(
                MIN_RADIUS_FRACTION.toDouble(),
                MAX_RADIUS_FRACTION.toDouble(),
            ).toFloat()
            var x = position.x
            var y = position.y

            for (attempt in 0 until 32) {
                val overlapsCenter = circlesOverlap(x, y, nodeRadius, centerX, centerY, centerRadius)
                val overlapsPeer = adjusted.withIndex().any { (peerIndex, peer) ->
                    peerIndex != index &&
                        circlesOverlap(x, y, nodeRadius, peer.x, peer.y, peer.radius)
                }
                if (!overlapsCenter && !overlapsPeer) {
                    adjusted[index] = position.copy(x = x, y = y)
                    break
                }
                fraction = (fraction + 0.03f).coerceAtMost(MAX_RADIUS_FRACTION.toFloat())
                x = centerX + orbitRadius * fraction * cos(angle).toFloat()
                y = centerY + orbitRadius * fraction * sin(angle).toFloat()
            }
            if (adjusted[index].x != x || adjusted[index].y != y) {
                adjusted[index] = position.copy(x = x, y = y)
            }
        }
        return adjusted
    }

    internal fun hasOverlap(
        dragged: NodePosition,
        others: List<NodePosition>,
        centerX: Float,
        centerY: Float,
        centerRadius: Float,
    ): Boolean {
        if (circlesOverlap(dragged.x, dragged.y, dragged.radius, centerX, centerY, centerRadius)) {
            return true
        }
        return others.any { other ->
            circlesOverlap(
                dragged.x,
                dragged.y,
                dragged.radius,
                other.x,
                other.y,
                other.radius,
            )
        }
    }
}
