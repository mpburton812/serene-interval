package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathStructureSpec
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.FillDirection
import com.example.meditationparticles.domain.breathing.SphereRoleKind
import com.example.meditationparticles.domain.breathing.computeStructureSpec
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

data class BreathPipe(
    val fromSphereId: Int,
    val toSphereId: Int,
)

data class BreathStructureLayout(
    val spec: BreathStructureSpec,
    val spheres: Map<Int, BreathSphere>,
    val inhalePath: List<Int>,
    val exhalePath: List<Int>,
    val topHoldId: Int?,
    val bottomHoldId: Int?,
    val pipes: List<BreathPipe>,
    val scale: Float,
    val layoutMode: LayoutMode,
) {
    val allSpheres: List<BreathSphere> get() = spheres.values.toList()

    fun sphere(id: Int): BreathSphere? = spheres[id]

    fun pipeEdgePoints(pipe: BreathPipe): Pair<Offset, Offset>? {
        val from = sphere(pipe.fromSphereId) ?: return null
        val to = sphere(pipe.toSphereId) ?: return null
        return pipeEdge(from, to) to pipeEdge(to, from)
    }
}

enum class LayoutMode {
    InterleavedLadder,
    FlowChain,
    ModeB,
}

data class SphereVisualState(
    val fillLevel: Float,
    val isActive: Boolean,
    val fillDirection: FillDirection = FillDirection.BottomToTop,
    val isDraining: Boolean = false,
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
    zoneFillRatio: Float = FLOW_CHAIN_FILL_RATIO,
): BreathStructureLayout {
    val spec = computeStructureSpec(pattern)
    val zoneTop = topInset + ZONE_PADDING
    val zoneBottom = height - bottomInset - ZONE_PADDING
    val zoneHeight = (zoneBottom - zoneTop).coerceAtLeast(80f)
    val zoneWidth = width - ZONE_PADDING * 2f
    val scale = min(width, height) / 900f

    val ladder = when (spec.layoutStrategy) {
        com.example.meditationparticles.domain.breathing.LayoutStrategy.InterleavedLadder ->
            tryBuildInterleavedLadder(spec, zoneWidth, zoneHeight, zoneTop, width / 2f, scale, zoneFillRatio)
        com.example.meditationparticles.domain.breathing.LayoutStrategy.FlowChain ->
            buildFlowChainLayout(spec, zoneWidth, zoneHeight, zoneTop, width / 2f, scale, zoneFillRatio)
    }
    if (ladder != null) return ladder

    return buildFlowChainLayout(spec, zoneWidth, zoneHeight, zoneTop, width / 2f, scale, zoneFillRatio)
}

private enum class ChainKind {
    BottomHold,
    Inhale,
    TopHold,
    Exhale,
}

private data class ChainEntry(
    val kind: ChainKind,
    val segmentIndex: Int = 0,
)

private fun buildFlowChainEntries(spec: BreathStructureSpec): List<ChainEntry> = buildList {
    if (spec.hasBottomHold) add(ChainEntry(ChainKind.BottomHold))
    repeat(spec.inhaleSphereCount) { add(ChainEntry(ChainKind.Inhale, it)) }
    if (spec.hasTopHold) add(ChainEntry(ChainKind.TopHold))
    repeat(spec.exhaleSphereCount) { add(ChainEntry(ChainKind.Exhale, it)) }
}

internal const val FLOW_CHAIN_FILL_RATIO = 0.94f
const val PREVIEW_ZONE_FILL_RATIO = 0.99f

private fun adaptiveFlowGap(nodeCount: Int): Float = when {
    nodeCount >= 12 -> 5f
    nodeCount >= 9 -> 6f
    else -> MIN_SPHERE_GAP
}

private data class FlowChainPlacement(
    val spheres: Map<Int, BreathSphere>,
    val chainIds: List<Int>,
    val inhalePath: List<Int>,
    val exhalePath: List<Int>,
    val topHoldId: Int?,
    val bottomHoldId: Int?,
    val topExtent: Float,
    val bottomExtent: Float,
)

private fun buildFlowChainLayout(
    spec: BreathStructureSpec,
    zoneWidth: Float,
    zoneHeight: Float,
    zoneTop: Float,
    centerX: Float,
    scale: Float,
    zoneFillRatio: Float = FLOW_CHAIN_FILL_RATIO,
): BreathStructureLayout {
    val chain = buildFlowChainEntries(spec)
    val gap = if (zoneFillRatio >= PREVIEW_ZONE_FILL_RATIO - 0.01f) {
        adaptiveFlowGap(chain.size).coerceAtMost(5f)
    } else {
        adaptiveFlowGap(chain.size)
    }

    val upwardEnd = chain.indexOfLast {
        it.kind == ChainKind.BottomHold || it.kind == ChainKind.Inhale || it.kind == ChainKind.TopHold
    } + 1
    val upwardChain = chain.subList(0, upwardEnd)
    val downwardChain = chain.subList(upwardEnd, chain.size)
    val zoneBottom = zoneTop + zoneHeight
    val targetSpan = zoneHeight * zoneFillRatio.coerceIn(0.5f, 1f)

    var columnOffset = (zoneWidth * 0.24f).coerceIn(36f, zoneWidth * 0.32f)
    var maxSmallR = min(
        min(zoneHeight / 3.5f, zoneWidth * 0.24f),
        zoneWidth / 2f - columnOffset,
    ).coerceAtLeast(MIN_SPHERE_RADIUS)

    fun holdRadius(small: Float) = small * HOLD_TO_SMALL_RATIO

    fun nodeRadius(kind: ChainKind, small: Float) =
        if (kind == ChainKind.BottomHold || kind == ChainKind.TopHold) holdRadius(small) else small

    fun placeAtRadius(small: Float): FlowChainPlacement {
        val holdR = holdRadius(small)
        var nextId = 0
        val spheres = mutableMapOf<Int, BreathSphere>()
        val chainIds = mutableListOf<Int>()
        val inhalePath = mutableListOf<Int>()
        val exhalePath = mutableListOf<Int>()
        var topHoldId: Int? = null
        var bottomHoldId: Int? = null
        var pathSideIndex = 0

        fun placeNode(entry: ChainEntry, centerY: Float) {
            val radius = nodeRadius(entry.kind, small)
            val x = if (entry.kind == ChainKind.BottomHold || entry.kind == ChainKind.TopHold) {
                centerX
            } else {
                val side = pathSideIndex % 2
                pathSideIndex++
                if (side == 0) centerX - columnOffset else centerX + columnOffset
            }
            val id = nextId++
            val role = when (entry.kind) {
                ChainKind.BottomHold, ChainKind.TopHold -> SphereRole.HoldPurple
                ChainKind.Inhale -> SphereRole.InhaleBlue
                ChainKind.Exhale -> SphereRole.ExhaleRed
            }
            spheres[id] = BreathSphere(id, Offset(x, centerY), radius, role)
            chainIds.add(id)
            when (entry.kind) {
                ChainKind.BottomHold -> bottomHoldId = id
                ChainKind.TopHold -> topHoldId = id
                ChainKind.Inhale -> inhalePath.add(id)
                ChainKind.Exhale -> exhalePath.add(id)
            }
        }

        var y = 0f
        upwardChain.forEach { entry ->
            val radius = nodeRadius(entry.kind, small)
            y -= radius
            placeNode(entry, y)
            y -= radius + gap
        }

        if (downwardChain.isNotEmpty()) {
            val peak = spheres.getValue(chainIds.last())
            y = peak.center.y + peak.radius + gap
        }

        downwardChain.forEach { entry ->
            val radius = nodeRadius(entry.kind, small)
            y += radius
            placeNode(entry, y)
            y += radius + gap
        }

        val topExtent = spheres.values.minOf { it.center.y - it.radius }
        val bottomExtent = spheres.values.maxOf { it.center.y + it.radius }
        return FlowChainPlacement(
            spheres = spheres,
            chainIds = chainIds,
            inhalePath = inhalePath,
            exhalePath = exhalePath,
            topHoldId = topHoldId,
            bottomHoldId = bottomHoldId,
            topExtent = topExtent,
            bottomExtent = bottomExtent,
        )
    }

    var low = MIN_SPHERE_RADIUS
    var high = maxSmallR
    var bestPlacement = placeAtRadius(MIN_SPHERE_RADIUS)

    repeat(14) {
        val mid = (low + high) / 2f
        val widthLimited = min(mid, zoneWidth / 2f - columnOffset).coerceAtLeast(MIN_SPHERE_RADIUS)
        if (widthLimited < mid) {
            columnOffset = (zoneWidth / 2f - widthLimited).coerceAtLeast(36f)
        }
        val trial = placeAtRadius(widthLimited)
        val span = trial.bottomExtent - trial.topExtent
        if (span <= targetSpan) {
            bestPlacement = trial
            low = mid
        } else {
            high = mid
        }
    }

    val zoneCenterY = zoneTop + zoneHeight / 2f
    val placementCenterY = (bestPlacement.topExtent + bestPlacement.bottomExtent) / 2f
    val yShift = zoneCenterY - placementCenterY

    var finalSpheres = bestPlacement.spheres.mapValues { (_, sphere) ->
        sphere.copy(center = Offset(sphere.center.x, sphere.center.y + yShift))
    }

    var topExtent = bestPlacement.topExtent + yShift
    var bottomExtent = bestPlacement.bottomExtent + yShift
    if (topExtent < zoneTop || bottomExtent > zoneBottom) {
        val clampShift = when {
            topExtent < zoneTop -> zoneTop - topExtent
            bottomExtent > zoneBottom -> zoneBottom - bottomExtent
            else -> 0f
        }
        finalSpheres = finalSpheres.mapValues { (_, sphere) ->
            sphere.copy(center = Offset(sphere.center.x, sphere.center.y + clampShift))
        }
    }

    val spheres = finalSpheres
    val inhalePath = bestPlacement.inhalePath
    val exhalePath = bestPlacement.exhalePath
    val topHoldId = bestPlacement.topHoldId
    val bottomHoldId = bestPlacement.bottomHoldId
    val chainIds = bestPlacement.chainIds

    val pipes = buildChainPipesFromIds(chainIds, spheres)

    return BreathStructureLayout(
        spec = spec,
        spheres = spheres,
        inhalePath = inhalePath,
        exhalePath = exhalePath,
        topHoldId = topHoldId,
        bottomHoldId = bottomHoldId,
        pipes = pipes,
        scale = scale,
        layoutMode = LayoutMode.FlowChain,
    )
}

private fun buildChainPipesFromIds(
    chainIds: List<Int>,
    spheres: Map<Int, BreathSphere>,
): List<BreathPipe> {
    if (chainIds.size < 2) return emptyList()
    return chainIds.zip(chainIds.drop(1)).map { (fromId, toId) -> BreathPipe(fromId, toId) }
}

private fun pipeEdge(from: BreathSphere, toward: BreathSphere): Offset {
    val dx = toward.center.x - from.center.x
    val dy = toward.center.y - from.center.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    return Offset(
        from.center.x + dx / len * from.radius,
        from.center.y + dy / len * from.radius,
    )
}

private fun tryBuildInterleavedLadder(
    spec: BreathStructureSpec,
    zoneWidth: Float,
    zoneHeight: Float,
    zoneTop: Float,
    centerX: Float,
    scale: Float,
    zoneFillRatio: Float = FLOW_CHAIN_FILL_RATIO,
): BreathStructureLayout? {
    val rows = spec.ladderRows
    val gap = if (zoneFillRatio >= PREVIEW_ZONE_FILL_RATIO - 0.01f) {
        MIN_SPHERE_GAP.coerceAtMost(6f)
    } else {
        MIN_SPHERE_GAP
    }
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

    var holdR = smallR * HOLD_TO_SMALL_RATIO
    var contentHeightFinal = holdSlots * holdR * 2f + rowCount * 2f * smallR + (rowCount + holdSlots + 1) * gap

    if (contentHeightFinal > zoneHeight) {
        val maxSmallR = ((zoneHeight - (rowCount + holdSlots + 1) * gap) /
            (rowCount * 2f + holdSlots * HOLD_TO_SMALL_RATIO)).coerceAtLeast(MIN_SPHERE_RADIUS)
        if (maxSmallR < MIN_SPHERE_RADIUS && rows > 2) return null
        smallR = min(smallR, maxSmallR).coerceAtLeast(MIN_SPHERE_RADIUS)
        holdR = smallR * HOLD_TO_SMALL_RATIO
        contentHeightFinal = holdSlots * holdR * 2f + rowCount * 2f * smallR + (rowCount + holdSlots + 1) * gap
    }

    val targetHeight = zoneHeight * zoneFillRatio.coerceIn(0.5f, 1f)
    val maxSmallR = min(
        min(zoneHeight / 3.5f, zoneWidth * 0.24f),
        zoneWidth / 2f - columnOffset,
    ).coerceAtLeast(MIN_SPHERE_RADIUS)
    if (contentHeightFinal < targetHeight) {
        var low = smallR
        var high = maxSmallR
        repeat(12) {
            val mid = (low + high) / 2f
            val trialHold = mid * HOLD_TO_SMALL_RATIO
            val trialHeight = holdSlots * trialHold * 2f + rowCount * 2f * mid + (rowCount + holdSlots + 1) * gap
            if (trialHeight <= targetHeight) {
                smallR = mid
                low = mid
            } else {
                high = mid
            }
        }
        holdR = smallR * HOLD_TO_SMALL_RATIO
        contentHeightFinal = holdSlots * holdR * 2f + rowCount * 2f * smallR + (rowCount + holdSlots + 1) * gap
    }

    val leftX = centerX - columnOffset
    val rightX = centerX + columnOffset

    val rowYs = buildList {
        var y = zoneTop + (zoneHeight - contentHeightFinal) / 2f
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
        layoutMode = LayoutMode.InterleavedLadder,
    )
}

private fun inhaleSlotOrder(count: Int, rowCount: Int): List<Pair<Int, Int>> =
    (0 until count).map { index ->
        val row = (rowCount - 1 - index).coerceAtLeast(0)
        val col = if (index % 2 == 0) 1 else 0
        row to col
    }

private fun exhaleSlotOrder(count: Int, rowCount: Int): List<Pair<Int, Int>> =
    (0 until count).map { index ->
        val row = index.coerceAtMost(rowCount - 1)
        val col = if (index % 2 == 0) 1 else 0
        row to col
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
): List<BreathPipe> {
    val pipes = mutableListOf<BreathPipe>()

    fun connect(fromId: Int, toId: Int) {
        if (spheres[fromId] != null && spheres[toId] != null) {
            pipes.add(BreathPipe(fromId, toId))
        }
    }

    topHoldId?.let { topId ->
        gridSlots[0 to 0]?.let { connect(topId, it) }
        gridSlots[0 to 1]?.let { connect(topId, it) }
    }

    for (r in 0 until rowCount - 1) {
        gridSlots[r to 0]?.let { aId ->
            gridSlots[r + 1 to 1]?.let { bId -> connect(aId, bId) }
        }
        gridSlots[r to 1]?.let { aId ->
            gridSlots[r + 1 to 0]?.let { bId -> connect(aId, bId) }
        }
    }

    bottomHoldId?.let { bottomId ->
        val lastRow = rowCount - 1
        gridSlots[lastRow to 0]?.let { connect(it, bottomId) }
        gridSlots[lastRow to 1]?.let { connect(it, bottomId) }
    }

    return pipes
}

fun computeSphereVisuals(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
): Map<Int, SphereVisualState> {
    val fills = layout.allSpheres.associate { it.id to SphereVisualState(0f, false) }.toMutableMap()
    val spec = layout.spec
    val pattern = sessionState.pattern

    when (sessionState.phase) {
        BreathPhase.Inhale -> {
            val elapsed = sessionState.phaseElapsedSeconds()
            val firstSegDur = spec.inhaleSegments.firstOrNull()?.durationSeconds ?: 1f
            val bridgedBottom = sessionState.cycleCount > 0 && elapsed < firstSegDur

            if (bridgedBottom && layout.bottomHoldId != null) {
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = layout.bottomHoldId,
                    targetId = layout.inhalePath.first(),
                    localProgress = (elapsed / firstSegDur).coerceIn(0f, 1f),
                    sourceDirection = FillDirection.BottomToTopHold,
                    targetDirection = FillDirection.BottomToTop,
                )
                if (elapsed >= firstSegDur) {
                    applyPathFill(
                        fills = fills,
                        path = layout.inhalePath,
                        segments = spec.inhaleSegments,
                        elapsedSec = elapsed,
                        direction = FillDirection.BottomToTop,
                        elapsedOffsetSec = firstSegDur,
                        leadingCompleteCount = 1,
                    )
                }
            } else if (bridgedBottom && layout.bottomHoldId == null && layout.exhalePath.isNotEmpty()) {
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = layout.exhalePath.last(),
                    targetId = layout.inhalePath.first(),
                    localProgress = (elapsed / firstSegDur).coerceIn(0f, 1f),
                    sourceDirection = FillDirection.TopToBottom,
                    targetDirection = FillDirection.BottomToTop,
                )
                if (elapsed >= firstSegDur) {
                    applyPathFill(
                        fills = fills,
                        path = layout.inhalePath,
                        segments = spec.inhaleSegments,
                        elapsedSec = elapsed,
                        direction = FillDirection.BottomToTop,
                        elapsedOffsetSec = firstSegDur,
                        leadingCompleteCount = 1,
                    )
                }
            } else {
                applyPathFill(
                    fills = fills,
                    path = layout.inhalePath,
                    segments = spec.inhaleSegments,
                    elapsedSec = elapsed,
                    direction = FillDirection.BottomToTop,
                )
            }
        }
        BreathPhase.SecondInhale -> {
            val firstCount = kotlin.math.ceil(pattern.inhaleSeconds.toDouble()).toInt()
            val priorElapsed = spec.inhaleSegments.take(firstCount).sumOf { it.durationSeconds.toDouble() }.toFloat()
            applyPathFill(
                fills = fills,
                path = layout.inhalePath,
                segments = spec.inhaleSegments,
                elapsedSec = priorElapsed + sessionState.phaseElapsedSeconds(),
                direction = FillDirection.BottomToTop,
            )
        }
        BreathPhase.HoldIn -> {
            val progress = sessionState.phaseProgress.coerceIn(0f, 1f)
            layout.inhalePath.dropLast(1).forEach { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }
            layout.inhalePath.lastOrNull()?.let { lastInhale ->
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = lastInhale,
                    targetId = requireNotNull(layout.topHoldId),
                    localProgress = progress,
                    sourceDirection = FillDirection.BottomToTop,
                    targetDirection = FillDirection.BottomToTopHold,
                )
            }
        }
        BreathPhase.Exhale -> {
            val elapsed = sessionState.phaseElapsedSeconds()
            val firstSegDur = spec.exhaleSegments.firstOrNull()?.durationSeconds ?: 1f
            layout.inhalePath.dropLast(1).forEach { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }

            val bridgedTop = elapsed < firstSegDur
            if (bridgedTop) {
                val local = (elapsed / firstSegDur).coerceIn(0f, 1f)
                if (layout.topHoldId != null) {
                    applyBridgeTransfer(
                        fills = fills,
                        sourceId = layout.topHoldId,
                        targetId = layout.exhalePath.first(),
                        localProgress = local,
                        sourceDirection = FillDirection.BottomToTopHold,
                        targetDirection = FillDirection.TopToBottom,
                    )
                } else {
                    layout.inhalePath.lastOrNull()?.let { lastInhale ->
                        applyBridgeTransfer(
                            fills = fills,
                            sourceId = lastInhale,
                            targetId = layout.exhalePath.first(),
                            localProgress = local,
                            sourceDirection = FillDirection.BottomToTop,
                            targetDirection = FillDirection.TopToBottom,
                        )
                    }
                }
                if (elapsed >= firstSegDur) {
                    applyPathFill(
                        fills = fills,
                        path = layout.exhalePath,
                        segments = spec.exhaleSegments,
                        elapsedSec = elapsed,
                        direction = FillDirection.TopToBottom,
                        elapsedOffsetSec = firstSegDur,
                        leadingCompleteCount = 1,
                    )
                }
            } else {
                layout.topHoldId?.let { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTopHold) }
                layout.inhalePath.lastOrNull()?.let { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }
                applyPathFill(
                    fills = fills,
                    path = layout.exhalePath,
                    segments = spec.exhaleSegments,
                    elapsedSec = elapsed,
                    direction = FillDirection.TopToBottom,
                )
            }
        }
        BreathPhase.HoldOut -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }
            layout.exhalePath.dropLast(1).forEach { fills[it] = SphereVisualState(0f, false, FillDirection.TopToBottom) }
            layout.topHoldId?.let { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTopHold) }
            val progress = sessionState.phaseProgress.coerceIn(0f, 1f)
            layout.exhalePath.lastOrNull()?.let { lastExhale ->
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = lastExhale,
                    targetId = requireNotNull(layout.bottomHoldId),
                    localProgress = progress,
                    sourceDirection = FillDirection.TopToBottom,
                    targetDirection = FillDirection.BottomToTopHold,
                )
            }
        }
        else -> Unit
    }

    return fills
}

private fun BreathingSessionState.phaseElapsedSeconds(): Float =
    phaseProgress.coerceIn(0f, 1f) * phaseDurationSeconds

private fun applyBridgeTransfer(
    fills: MutableMap<Int, SphereVisualState>,
    sourceId: Int,
    targetId: Int,
    localProgress: Float,
    sourceDirection: FillDirection,
    targetDirection: FillDirection,
) {
    val transfer = localProgress.coerceIn(0f, 1f)
    fills[sourceId] = SphereVisualState(
        fillLevel = 1f - transfer,
        isActive = transfer in 0.001f..0.999f,
        fillDirection = sourceDirection,
        isDraining = transfer in 0.001f..0.999f,
    )
    fills[targetId] = SphereVisualState(
        fillLevel = transfer,
        isActive = transfer in 0.001f..0.999f,
        fillDirection = targetDirection,
        isDraining = false,
    )
}

private fun applyPathFill(
    fills: MutableMap<Int, SphereVisualState>,
    path: List<Int>,
    segments: List<com.example.meditationparticles.domain.breathing.PhaseSegment>,
    elapsedSec: Float,
    direction: FillDirection,
    elapsedOffsetSec: Float = 0f,
    leadingCompleteCount: Int = 0,
) {
    if (path.isEmpty()) return
    val elapsed = (elapsedSec - elapsedOffsetSec).coerceAtLeast(0f)
    if (elapsed <= 0f) return

    val completedCount = leadingCompleteCount.coerceIn(0, path.size)
    path.take(completedCount).forEach { sphereId ->
        fills[sphereId] = SphereVisualState(1f, false, direction)
    }

    var timeAcc = 0f
    var activeIndex = -1
    var localProgress = 0f

    path.forEachIndexed { index, _ ->
        if (index < completedCount) return@forEachIndexed
        val segDur = segments.getOrNull(index)?.durationSeconds ?: 1f
        if (activeIndex == -1 && elapsed < timeAcc + segDur) {
            activeIndex = index
            localProgress = ((elapsed - timeAcc) / segDur).coerceIn(0f, 1f)
        }
        timeAcc += segDur
    }

    if (activeIndex == -1) {
        path.forEachIndexed { index, sphereId ->
            fills[sphereId] = SphereVisualState(
                fillLevel = if (index == path.lastIndex) 1f else 0f,
                isActive = false,
                fillDirection = direction,
            )
        }
        return
    }

    path.forEachIndexed { index, sphereId ->
        fills[sphereId] = when {
            index < activeIndex - 1 -> SphereVisualState(0f, false, direction)
            index == activeIndex - 1 -> SphereVisualState(
                fillLevel = 1f - localProgress,
                isActive = localProgress in 0.001f..0.999f,
                fillDirection = direction,
                isDraining = localProgress in 0.001f..0.999f,
            )
            index == activeIndex -> SphereVisualState(
                fillLevel = localProgress,
                isActive = localProgress in 0.001f..0.999f,
                fillDirection = direction,
                isDraining = false,
            )
            index < completedCount -> SphereVisualState(1f, false, direction)
            else -> SphereVisualState(0f, false, direction)
        }
    }
}

fun breathingStartSphereId(layout: BreathStructureLayout): Int? = layout.inhalePath.firstOrNull()

fun computePreviewSphereVisuals(layout: BreathStructureLayout): Map<Int, SphereVisualState> {
    return layout.allSpheres.associate { sphere ->
        val direction = when (sphere.role) {
            SphereRole.InhaleBlue -> FillDirection.BottomToTop
            SphereRole.ExhaleRed -> FillDirection.TopToBottom
            SphereRole.HoldPurple -> FillDirection.BottomToTopHold
        }
        sphere.id to SphereVisualState(fillLevel = 1f, isActive = false, fillDirection = direction)
    }
}

fun computeModeBLayout(
    pattern: BreathingPattern,
    width: Float,
    height: Float,
    topInset: Float = 0f,
    bottomInset: Float = 0f,
    zoneFillRatio: Float = FLOW_CHAIN_FILL_RATIO,
): BreathStructureLayout {
    val spec = computeStructureSpec(pattern)
    val zoneTop = topInset + ZONE_PADDING
    val zoneBottom = height - bottomInset - ZONE_PADDING
    val zoneHeight = (zoneBottom - zoneTop).coerceAtLeast(80f)
    val zoneWidth = width - ZONE_PADDING * 2f
    val centerX = width / 2f
    val zoneCenterY = zoneTop + zoneHeight / 2f
    val scale = min(width, height) / 900f
    val gap = MIN_SPHERE_GAP
    val targetSpan = zoneHeight * zoneFillRatio.coerceIn(0.5f, 1f)

    fun placementFits(radius: Float): Boolean {
        val span = modeBVerticalSpan(spec, radius, gap)
        val layoutWidth = modeBLayoutWidth(spec, radius, gap)
        return span <= targetSpan && layoutWidth <= zoneWidth
    }

    var low = MIN_SPHERE_RADIUS
    var high = min(zoneHeight / 3f, zoneWidth / 4f).coerceAtLeast(MIN_SPHERE_RADIUS)
    var bestRadius = MIN_SPHERE_RADIUS

    repeat(14) {
        val mid = (low + high) / 2f
        if (placementFits(mid)) {
            bestRadius = mid
            low = mid
        } else {
            high = mid
        }
    }

    val placed = placeModeBSpheres(spec, bestRadius, gap, centerX, zoneCenterY)
    val yShift = zoneCenterY - (placed.topExtent + placed.bottomExtent) / 2f
    val spheres = placed.spheres.mapValues { (_, sphere) ->
        sphere.copy(center = Offset(sphere.center.x, sphere.center.y + yShift))
    }

    return BreathStructureLayout(
        spec = spec,
        spheres = spheres,
        inhalePath = placed.inhalePath,
        exhalePath = placed.exhalePath,
        topHoldId = placed.topHoldId,
        bottomHoldId = placed.bottomHoldId,
        pipes = buildModeBPipes(placed),
        scale = scale,
        layoutMode = LayoutMode.ModeB,
    )
}

private data class ModeBPlacement(
    val spheres: Map<Int, BreathSphere>,
    val inhalePath: List<Int>,
    val exhalePath: List<Int>,
    val topHoldId: Int?,
    val bottomHoldId: Int?,
    val topExtent: Float,
    val bottomExtent: Float,
)

private fun modeBLayoutWidth(spec: BreathStructureSpec, radius: Float, gap: Float): Float {
    return if (!spec.hasTopHold && !spec.hasBottomHold) {
        radius * 4f + gap
    } else {
        radius * 4f + gap
    }
}

private fun modeBVerticalSpan(spec: BreathStructureSpec, radius: Float, gap: Float): Float {
    val edge = radius * 2f
    val arm = radius * 2f + gap
    return when {
        spec.hasTopHold && spec.hasBottomHold -> edge + arm + arm + edge
        spec.hasTopHold -> edge + arm + edge
        else -> edge
    }
}

private fun placeModeBSpheres(
    spec: BreathStructureSpec,
    radius: Float,
    gap: Float,
    centerX: Float,
    centerY: Float,
): ModeBPlacement {
    val spheres = mutableMapOf<Int, BreathSphere>()
    var nextId = 0
    val arm = radius * 2f + gap

    val inhaleId = nextId++
    spheres[inhaleId] = BreathSphere(
        id = inhaleId,
        center = Offset(0f, 0f),
        radius = radius,
        role = SphereRole.InhaleBlue,
    )

    val exhaleId = nextId++
    spheres[exhaleId] = BreathSphere(
        id = exhaleId,
        center = Offset(0f, 0f),
        radius = radius,
        role = SphereRole.ExhaleRed,
    )

    var topHoldId: Int? = null
    var bottomHoldId: Int? = null

    if (spec.hasTopHold) {
        topHoldId = nextId++
        spheres[topHoldId] = BreathSphere(
            id = topHoldId,
            center = Offset(0f, 0f),
            radius = radius,
            role = SphereRole.HoldPurple,
        )
    }

    if (spec.hasBottomHold) {
        bottomHoldId = nextId++
        spheres[bottomHoldId] = BreathSphere(
            id = bottomHoldId,
            center = Offset(0f, 0f),
            radius = radius,
            role = SphereRole.HoldPurple,
        )
    }

    when {
        spec.hasTopHold && spec.hasBottomHold -> {
            spheres[inhaleId] = spheres.getValue(inhaleId).copy(center = Offset(centerX - arm / 2f, centerY))
            spheres[exhaleId] = spheres.getValue(exhaleId).copy(center = Offset(centerX + arm / 2f, centerY))
            spheres[topHoldId!!] = spheres.getValue(topHoldId).copy(center = Offset(centerX, centerY - arm))
            spheres[bottomHoldId!!] = spheres.getValue(bottomHoldId).copy(center = Offset(centerX, centerY + arm))
        }
        spec.hasTopHold -> {
            spheres[inhaleId] = spheres.getValue(inhaleId).copy(center = Offset(centerX - arm / 2f, centerY + arm / 4f))
            spheres[exhaleId] = spheres.getValue(exhaleId).copy(center = Offset(centerX + arm / 2f, centerY + arm / 4f))
            spheres[topHoldId!!] = spheres.getValue(topHoldId).copy(center = Offset(centerX, centerY - arm * 0.75f))
        }
        else -> {
            val halfSpan = arm / 2f
            spheres[inhaleId] = spheres.getValue(inhaleId).copy(center = Offset(centerX - halfSpan, centerY))
            spheres[exhaleId] = spheres.getValue(exhaleId).copy(center = Offset(centerX + halfSpan, centerY))
        }
    }

    val topExtent = spheres.values.minOf { it.center.y - it.radius }
    val bottomExtent = spheres.values.maxOf { it.center.y + it.radius }

    return ModeBPlacement(
        spheres = spheres,
        inhalePath = listOf(inhaleId),
        exhalePath = listOf(exhaleId),
        topHoldId = topHoldId,
        bottomHoldId = bottomHoldId,
        topExtent = topExtent,
        bottomExtent = bottomExtent,
    )
}

private fun buildModeBPipes(placement: ModeBPlacement): List<BreathPipe> {
    val inhaleId = placement.inhalePath.first()
    val exhaleId = placement.exhalePath.first()
    val topHoldId = placement.topHoldId
    val bottomHoldId = placement.bottomHoldId

    return when {
        topHoldId != null && bottomHoldId != null -> listOf(
            BreathPipe(bottomHoldId, inhaleId),
            BreathPipe(inhaleId, topHoldId),
            BreathPipe(topHoldId, exhaleId),
            BreathPipe(exhaleId, bottomHoldId),
        )
        topHoldId != null -> listOf(
            BreathPipe(inhaleId, topHoldId),
            BreathPipe(topHoldId, exhaleId),
        )
        else -> listOf(BreathPipe(inhaleId, exhaleId))
    }
}

fun computeModeBSphereVisuals(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
): Map<Int, SphereVisualState> {
    val fills = layout.allSpheres.associate { it.id to SphereVisualState(0f, false) }.toMutableMap()
    val progress = sessionState.phaseProgress.coerceIn(0f, 1f)
    val pattern = sessionState.pattern
    val inhaleId = layout.inhalePath.firstOrNull() ?: return fills
    val exhaleId = layout.exhalePath.firstOrNull() ?: return fills

    when (sessionState.phase) {
        BreathPhase.Inhale -> {
            if (layout.bottomHoldId != null && sessionState.cycleCount > 0) {
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = layout.bottomHoldId,
                    targetId = inhaleId,
                    localProgress = progress,
                    sourceDirection = FillDirection.BottomToTopHold,
                    targetDirection = FillDirection.BottomToTop,
                )
            } else if (pattern.secondInhaleSeconds > 0f) {
                val totalInhale = pattern.inhaleSeconds + pattern.secondInhaleSeconds
                val fill = (progress * pattern.inhaleSeconds / totalInhale).coerceIn(0f, 1f)
                fills[inhaleId] = SphereVisualState(
                    fillLevel = fill,
                    isActive = fill in 0.001f..0.999f,
                    fillDirection = FillDirection.BottomToTop,
                )
            } else {
                fills[inhaleId] = SphereVisualState(
                    fillLevel = progress,
                    isActive = progress in 0.001f..0.999f,
                    fillDirection = FillDirection.BottomToTop,
                )
            }
        }
        BreathPhase.SecondInhale -> {
            val totalInhale = pattern.inhaleSeconds + pattern.secondInhaleSeconds
            val fill = ((pattern.inhaleSeconds + progress * pattern.secondInhaleSeconds) / totalInhale)
                .coerceIn(0f, 1f)
            fills[inhaleId] = SphereVisualState(
                fillLevel = fill,
                isActive = fill in 0.001f..0.999f,
                fillDirection = FillDirection.BottomToTop,
            )
        }
        BreathPhase.HoldIn -> {
            layout.topHoldId?.let { holdId ->
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = inhaleId,
                    targetId = holdId,
                    localProgress = progress,
                    sourceDirection = FillDirection.BottomToTop,
                    targetDirection = FillDirection.BottomToTopHold,
                )
            }
        }
        BreathPhase.Exhale -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }
            val sourceId = layout.topHoldId ?: inhaleId
            val sourceDirection = if (layout.topHoldId != null) {
                FillDirection.BottomToTopHold
            } else {
                FillDirection.BottomToTop
            }
            applyBridgeTransfer(
                fills = fills,
                sourceId = sourceId,
                targetId = exhaleId,
                localProgress = progress,
                sourceDirection = sourceDirection,
                targetDirection = FillDirection.TopToBottom,
            )
        }
        BreathPhase.HoldOut -> {
            layout.inhalePath.forEach { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTop) }
            layout.topHoldId?.let { fills[it] = SphereVisualState(0f, false, FillDirection.BottomToTopHold) }
            layout.bottomHoldId?.let { holdId ->
                applyBridgeTransfer(
                    fills = fills,
                    sourceId = exhaleId,
                    targetId = holdId,
                    localProgress = progress,
                    sourceDirection = FillDirection.TopToBottom,
                    targetDirection = FillDirection.BottomToTopHold,
                )
            }
        }
        else -> Unit
    }

    return fills
}

fun activeMoteSphere(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
    visuals: Map<Int, SphereVisualState>,
): BreathSphere? {
    return when (sessionState.phase) {
        BreathPhase.Inhale, BreathPhase.SecondInhale -> {
            layout.inhalePath.firstOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.exhalePath.lastOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.bottomHoldId?.takeIf { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        BreathPhase.HoldIn -> {
            layout.topHoldId?.takeIf { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.inhalePath.lastOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        BreathPhase.Exhale -> {
            layout.exhalePath.firstOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.topHoldId?.takeIf { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.inhalePath.lastOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        BreathPhase.HoldOut -> {
            layout.bottomHoldId?.takeIf { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
                ?: layout.exhalePath.lastOrNull { visuals[it]?.isActive == true }?.let { layout.sphere(it) }
        }
        else -> null
    }
}
