package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathStructureSpec
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.FillDirection
import com.example.meditationparticles.domain.breathing.SphereRoleKind
import com.example.meditationparticles.domain.breathing.computeStructureSpec
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class SphereRole {
    HoldPurple,
    InhaleBlue,
    ExhaleRed,
}

fun SphereRoleKind.toSphereRole(): SphereRole = when (this) {
    SphereRoleKind.InhaleBlue -> SphereRole.InhaleBlue
    SphereRoleKind.ExhaleRed -> SphereRole.ExhaleRed
    SphereRoleKind.HoldPurple -> SphereRole.HoldPurple
}

data class BreathSphere(
    val id: Int,
    val center: Offset,
    val radius: Float,
    val role: SphereRole,
)

data class BreathStructureLayout(
    val spec: BreathStructureSpec,
    val spheres: Map<Int, BreathSphere>,
    val inhalePath: List<Int>,
    val exhalePath: List<Int>,
    val topHoldId: Int?,
    val bottomHoldId: Int?,
    val pipes: List<Pair<Offset, Offset>>,
    val scale: Float,
    val layoutMode: LayoutMode,
) {
    val allSpheres: List<BreathSphere> get() = spheres.values.toList()

    fun sphere(id: Int): BreathSphere? = spheres[id]
}

enum class LayoutMode {
    Ladder,
    Compact,
}

data class SphereVisualState(
    val fillLevel: Float,
    val isActive: Boolean,
    val fillDirection: FillDirection = FillDirection.BottomToTop,
)

private const val HOLD_TO_SMALL_RATIO = 1.1f
private const val MIN_SPHERE_GAP = 8f
private const val ZONE_PADDING = 10f
private const val MIN_SPHERE_RADIUS = 8f

fun computeStructureLayout(
    pattern: BreathingPattern,
    width: Float,
    height: Float,
    topInset: Float = 0f,
    bottomInset: Float = 0f,
): BreathStructureLayout {
    val spec = computeStructureSpec(pattern)
    val zoneTop = topInset + ZONE_PADDING
    val zoneBottom = height - bottomInset - ZONE_PADDING
    val zoneHeight = (zoneBottom - zoneTop).coerceAtLeast(80f)
    val zoneWidth = width - ZONE_PADDING * 2f
    val scale = min(width, height) / 900f

    val ladder = tryBuildLadder(spec, zoneWidth, zoneHeight, zoneTop, width / 2f, scale)
    if (ladder != null) return ladder

    return buildCompactLayout(spec, zoneWidth, zoneHeight, zoneTop, width / 2f, scale)
}

private fun tryBuildLadder(
    spec: BreathStructureSpec,
    zoneWidth: Float,
    zoneHeight: Float,
    zoneTop: Float,
    centerX: Float,
    scale: Float,
): BreathStructureLayout? {
    val rows = spec.ladderRows
    val gap = MIN_SPHERE_GAP
    val holdSlots = (if (spec.hasTopHold) 1 else 0) + (if (spec.hasBottomHold) 1 else 0)
    val rowCount = rows

    val heightDenom = rowCount * 2f + holdSlots * HOLD_TO_SMALL_RATIO + (rowCount + holdSlots - 1).coerceAtLeast(0) * (gap / 16f)
    var smallR = ((zoneHeight - (rowCount + holdSlots + 1) * gap) / (rowCount * 2f + holdSlots * HOLD_TO_SMALL_RATIO)).coerceAtLeast(MIN_SPHERE_RADIUS)

    var columnOffset = (zoneWidth * 0.27f).coerceIn(40f, zoneWidth * 0.36f)
    var smallRFromWidth = zoneWidth / 2f - columnOffset
    if (smallRFromWidth < smallR) {
        columnOffset = (zoneWidth / 2f - smallR).coerceAtLeast(36f)
        smallRFromWidth = zoneWidth / 2f - columnOffset
    }
    smallR = min(smallR, smallRFromWidth).coerceAtLeast(MIN_SPHERE_RADIUS)

    val holdR = smallR * HOLD_TO_SMALL_RATIO
    val contentHeight = holdSlots * holdR * 2f + rowCount * 2f * smallR + (rowCount + holdSlots + 1) * gap
    if (contentHeight > zoneHeight * 1.08f && rows > 2) return null

    val leftX = centerX - columnOffset
    val rightX = centerX + columnOffset

    val rowYs = buildList {
        var y = zoneTop + (zoneHeight - contentHeight) / 2f
        if (spec.hasTopHold) {
            y += holdR
            add(y)
            y += holdR + gap + smallR
        } else {
            y += smallR
        }
        repeat(rowCount) {
            add(y)
            y += 2f * smallR + gap
        }
        if (spec.hasBottomHold) {
            y -= smallR
            y += gap + holdR
            add(y)
        }
    }

    val inhaleSlots = inhaleSlotOrder(spec.inhaleSphereCount, rowCount).toSet()
    val exhaleSlots = exhaleSlotOrder(spec.exhaleSphereCount, rowCount).toSet()

    var nextId = 0
    val spheres = mutableMapOf<Int, BreathSphere>()
    val gridSlots = mutableMapOf<Pair<Int, Int>, Int>()

    fun slot(row: Int, col: Int, role: SphereRole, y: Float, x: Float, radius: Float) {
        val id = nextId++
        spheres[id] = BreathSphere(id, Offset(x, y), radius, role)
        gridSlots[row to col] = id
    }

    var topHoldId: Int? = null
    var bottomHoldId: Int? = null

    if (spec.hasTopHold) {
        topHoldId = nextId
        slot(-1, 0, SphereRole.HoldPurple, rowYs[0], centerX, holdR)
    }

    val firstRowIdx = if (spec.hasTopHold) 1 else 0
    for (r in 0 until rowCount) {
        val y = rowYs[firstRowIdx + r]
        if (r to 0 in inhaleSlots) {
            slot(r, 0, SphereRole.InhaleBlue, y, leftX, smallR)
        } else if (r to 0 in exhaleSlots) {
            slot(r, 0, SphereRole.ExhaleRed, y, leftX, smallR)
        }
        if (r to 1 in inhaleSlots) {
            slot(r, 1, SphereRole.InhaleBlue, y, rightX, smallR)
        } else if (r to 1 in exhaleSlots) {
            slot(r, 1, SphereRole.ExhaleRed, y, rightX, smallR)
        }
    }

    if (spec.hasBottomHold) {
        bottomHoldId = nextId
        slot(rowCount, 0, SphereRole.HoldPurple, rowYs.last(), centerX, holdR)
    }

    val inhalePath = buildInhalePathIds(spec.inhaleSphereCount, rowCount, gridSlots)
    val exhalePath = buildExhalePathIds(spec.exhaleSphereCount, rowCount, gridSlots)

    val pipes = buildLadderPipes(
        spheres = spheres,
        topHoldId = topHoldId,
        bottomHoldId = bottomHoldId,
        rowCount = rowCount,
        gridSlots = gridSlots,
        centerX = centerX,
    )

    return BreathStructureLayout(
        spec = spec,
        spheres = spheres,
        inhalePath = inhalePath,
        exhalePath = exhalePath,
        topHoldId = topHoldId,
        bottomHoldId = bottomHoldId,
        pipes = pipes,
        scale = scale,
        layoutMode = LayoutMode.Ladder,
    )
}

private fun buildCompactLayout(
    spec: BreathStructureSpec,
    zoneWidth: Float,
    zoneHeight: Float,
    zoneTop: Float,
    centerX: Float,
    scale: Float,
): BreathStructureLayout {
    val gap = MIN_SPHERE_GAP
    val totalPathSpheres = spec.inhaleSphereCount + spec.exhaleSphereCount
    val holdSlots = (if (spec.hasTopHold) 1 else 0) + (if (spec.hasBottomHold) 1 else 0)
    val totalNodes = totalPathSpheres + holdSlots
    val smallR = ((zoneHeight - (totalNodes + 1) * gap) / (totalNodes * 2f)).coerceIn(MIN_SPHERE_RADIUS, zoneWidth * 0.22f)
    val holdR = smallR * HOLD_TO_SMALL_RATIO

    var nextId = 0
    val spheres = mutableMapOf<Int, BreathSphere>()
    val orderedCenters = mutableListOf<Offset>()

    var y = zoneTop + zoneHeight - (if (spec.hasBottomHold) holdR else smallR)

    var topHoldId: Int? = null
    var bottomHoldId: Int? = null

    if (spec.hasBottomHold) {
        bottomHoldId = nextId
        val id = nextId++
        spheres[id] = BreathSphere(id, Offset(centerX, y), holdR, SphereRole.HoldPurple)
        orderedCenters.add(Offset(centerX, y))
        y -= holdR + gap + smallR
    } else {
        y = zoneTop + zoneHeight - smallR
    }

    val inhaleIds = mutableListOf<Int>()
    spec.inhaleSegments.forEachIndexed { index, _ ->
        val x = if (index % 2 == 0) centerX - zoneWidth * 0.22f else centerX + zoneWidth * 0.22f
        val id = nextId++
        spheres[id] = BreathSphere(id, Offset(x, y), smallR, SphereRole.InhaleBlue)
        inhaleIds.add(id)
        orderedCenters.add(Offset(x, y))
        y -= smallR * 2f + gap
    }

    val exhaleIdsReversed = mutableListOf<Int>()
    spec.exhaleSegments.forEachIndexed { index, _ ->
        val x = if (index % 2 == 1) centerX - zoneWidth * 0.22f else centerX + zoneWidth * 0.22f
        val id = nextId++
        spheres[id] = BreathSphere(id, Offset(x, y), smallR, SphereRole.ExhaleRed)
        exhaleIdsReversed.add(0, id)
        orderedCenters.add(0, Offset(x, y))
        y -= smallR * 2f + gap
    }

    if (spec.hasTopHold) {
        topHoldId = nextId
        val topY = zoneTop + holdR
        spheres[nextId] = BreathSphere(nextId, Offset(centerX, topY), holdR, SphereRole.HoldPurple)
        orderedCenters.add(0, Offset(centerX, topY))
        nextId++
    }

    val pipes = buildChainPipes(orderedCenters.sortedBy { it.y })

    return BreathStructureLayout(
        spec = spec,
        spheres = spheres,
        inhalePath = inhaleIds,
        exhalePath = exhaleIdsReversed,
        topHoldId = topHoldId,
        bottomHoldId = bottomHoldId,
        pipes = pipes,
        scale = scale,
        layoutMode = LayoutMode.Compact,
    )
}

private fun inhaleSlotOrder(count: Int, rowCount: Int): List<Pair<Int, Int>> {
    if (count == 0) return emptyList()
    val order = mutableListOf<Pair<Int, Int>>()
    var placed = 0
    var row = rowCount - 1
    while (placed < count && row >= 0) {
        order.add(row to 1)
        placed++
        if (placed >= count) break
        if (row > 0) {
            order.add(row - 1 to 0)
            placed++
            row--
        } else {
            break
        }
    }
    while (placed < count) {
        order.add(0 to 0)
        placed++
    }
    return order.take(count)
}

private fun exhaleSlotOrder(count: Int, rowCount: Int): List<Pair<Int, Int>> {
    if (count == 0) return emptyList()
    val order = mutableListOf<Pair<Int, Int>>()
    var placed = 0
    var row = 0
    while (placed < count && row < rowCount) {
        order.add(row to 1)
        placed++
        if (placed >= count) break
        if (row < rowCount - 1) {
            order.add(row + 1 to 0)
            placed++
            row++
        } else {
            break
        }
    }
    while (placed < count) {
        order.add(rowCount - 1 to 0)
        placed++
    }
    return order.take(count)
}

private fun buildInhalePathIds(count: Int, rowCount: Int, gridSlots: Map<Pair<Int, Int>, Int>): List<Int> {
    return inhaleSlotOrder(count, rowCount).mapNotNull { gridSlots[it] }
}

private fun buildExhalePathIds(count: Int, rowCount: Int, gridSlots: Map<Pair<Int, Int>, Int>): List<Int> {
    return exhaleSlotOrder(count, rowCount).mapNotNull { gridSlots[it] }
}

private fun buildLadderPipes(
    spheres: Map<Int, BreathSphere>,
    topHoldId: Int?,
    bottomHoldId: Int?,
    rowCount: Int,
    gridSlots: Map<Pair<Int, Int>, Int>,
    centerX: Float,
): List<Pair<Offset, Offset>> {
    val pipes = mutableListOf<Pair<Offset, Offset>>()
    fun edge(from: BreathSphere, toward: Offset): Offset {
        val dx = toward.x - from.center.x
        val dy = toward.y - from.center.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        return Offset(from.center.x + dx / len * from.radius * 0.92f, from.center.y + dy / len * from.radius * 0.92f)
    }

    topHoldId?.let { topId ->
        val top = spheres[topId] ?: return@let
        gridSlots[0 to 0]?.let { id -> spheres[id]?.let { pipes.add(edge(top, it.center) to it.center) } }
        gridSlots[0 to 1]?.let { id -> spheres[id]?.let { pipes.add(edge(top, it.center) to it.center) } }
    }

    for (r in 0 until rowCount - 1) {
        gridSlots[r to 0]?.let { aId ->
            gridSlots[r + 1 to 1]?.let { bId ->
                spheres[aId]?.center?.let { a -> spheres[bId]?.center?.let { b -> pipes.add(a to b) } }
            }
        }
        gridSlots[r to 1]?.let { aId ->
            gridSlots[r + 1 to 0]?.let { bId ->
                spheres[aId]?.center?.let { a -> spheres[bId]?.center?.let { b -> pipes.add(a to b) } }
            }
        }
    }

    bottomHoldId?.let { bottomId ->
        val bottom = spheres[bottomId] ?: return@let
        val lastRow = rowCount - 1
        gridSlots[lastRow to 0]?.let { id -> spheres[id]?.let { pipes.add(it.center to edge(bottom, it.center)) } }
        gridSlots[lastRow to 1]?.let { id -> spheres[id]?.let { pipes.add(it.center to edge(bottom, it.center)) } }
    }

    return pipes
}

private fun buildChainPipes(centers: List<Offset>): List<Pair<Offset, Offset>> {
    if (centers.size < 2) return emptyList()
    return centers.zip(centers.drop(1))
}

fun computeSphereVisuals(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
): Map<Int, SphereVisualState> {
    val fills = layout.allSpheres.associate { it.id to SphereVisualState(0f, false) }.toMutableMap()
    val spec = layout.spec

    when (sessionState.phase) {
        BreathPhase.Inhale -> {
            applyPathFill(
                fills = fills,
                path = layout.inhalePath,
                segments = spec.inhaleSegments,
                phaseProgress = sessionState.phaseProgress,
                phaseDurationSec = sessionState.phaseDurationSeconds,
                direction = FillDirection.BottomToTop,
            )
        }
        BreathPhase.SecondInhale -> {
            val firstCount = kotlin.math.ceil(sessionState.pattern.inhaleSeconds.toDouble()).toInt()
            layout.inhalePath.take(firstCount).forEach {
                fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTop)
            }
            applyPathFill(
                fills = fills,
                path = layout.inhalePath.drop(firstCount),
                segments = spec.inhaleSegments.drop(firstCount),
                phaseProgress = sessionState.phaseProgress,
                phaseDurationSec = sessionState.phaseDurationSeconds,
                direction = FillDirection.BottomToTop,
            )
        }
        BreathPhase.HoldIn -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTop) }
            layout.topHoldId?.let { id ->
                fills[id] = holdFill(sessionState.phaseProgress, FillDirection.BottomToTopHold)
            }
        }
        BreathPhase.Exhale -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTop) }
            layout.topHoldId?.let { fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTopHold) }
            applyPathFill(
                fills = fills,
                path = layout.exhalePath,
                segments = spec.exhaleSegments,
                phaseProgress = sessionState.phaseProgress,
                phaseDurationSec = sessionState.phaseDurationSeconds,
                direction = FillDirection.TopToBottom,
            )
        }
        BreathPhase.HoldOut -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTop) }
            layout.exhalePath.forEach { fills[it] = SphereVisualState(1f, false, FillDirection.TopToBottom) }
            layout.topHoldId?.let { fills[it] = SphereVisualState(1f, false, FillDirection.BottomToTopHold) }
            layout.bottomHoldId?.let { id ->
                fills[id] = holdFill(sessionState.phaseProgress, FillDirection.BottomToTopHold)
            }
        }
        else -> Unit
    }

    return fills
}

private fun applyPathFill(
    fills: MutableMap<Int, SphereVisualState>,
    path: List<Int>,
    segments: List<com.example.meditationparticles.domain.breathing.PhaseSegment>,
    phaseProgress: Float,
    phaseDurationSec: Float,
    direction: FillDirection,
) {
    if (path.isEmpty()) return
    val elapsed = phaseProgress.coerceIn(0f, 1f) * phaseDurationSec
    var timeAcc = 0f

    path.forEachIndexed { index, sphereId ->
        val segDur = segments.getOrNull(index)?.durationSeconds ?: 1f
        when {
            elapsed >= timeAcc + segDur -> {
                fills[sphereId] = SphereVisualState(1f, false, direction)
            }
            elapsed > timeAcc -> {
                val local = ((elapsed - timeAcc) / segDur).coerceIn(0f, 1f)
                fills[sphereId] = SphereVisualState(local, true, direction)
            }
            else -> {
                fills[sphereId] = SphereVisualState(0f, false, direction)
            }
        }
        timeAcc += segDur
    }
}

private fun holdFill(phaseProgress: Float, direction: FillDirection): SphereVisualState {
    val fill = phaseProgress.coerceIn(0f, 1f)
    return SphereVisualState(fill, fill < 1f, direction)
}

fun activeMoteSphere(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
    visuals: Map<Int, SphereVisualState>,
): BreathSphere? {
    return when (sessionState.phase) {
        BreathPhase.Inhale, BreathPhase.SecondInhale -> {
            layout.inhalePath.firstOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        BreathPhase.HoldIn -> {
            layout.topHoldId?.takeIf { (visuals[it]?.fillLevel ?: 0f) < 1f }?.let { layout.sphere(it) }
        }
        BreathPhase.Exhale -> {
            layout.exhalePath.firstOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        BreathPhase.HoldOut -> {
            layout.bottomHoldId?.takeIf { (visuals[it]?.fillLevel ?: 0f) < 1f }?.let { layout.sphere(it) }
        }
        else -> null
    }
}
