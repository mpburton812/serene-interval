package com.example.meditationparticles.domain.breathing

import kotlin.math.ceil
import kotlin.math.min

enum class FillDirection {
    BottomToTop,
    TopToBottom,
    BottomToTopHold,
}

data class PhaseSegment(
    val role: SphereRoleKind,
    val durationSeconds: Float,
)

enum class SphereRoleKind {
    InhaleBlue,
    ExhaleRed,
    HoldPurple,
}

data class BreathStructureSpec(
    val inhaleSegments: List<PhaseSegment>,
    val exhaleSegments: List<PhaseSegment>,
    val topHoldSeconds: Float?,
    val bottomHoldSeconds: Float?,
) {
    val inhaleSphereCount: Int get() = inhaleSegments.size
    val exhaleSphereCount: Int get() = exhaleSegments.size
    val hasTopHold: Boolean get() = topHoldSeconds != null
    val hasBottomHold: Boolean get() = bottomHoldSeconds != null

    val ladderRows: Int
        get() = requiredLadderRows(inhaleSphereCount, exhaleSphereCount)
}

/** Minimum ladder rows so every inhale/exhale sphere maps to a unique, non-overlapping grid slot. */
fun requiredLadderRows(inhaleCount: Int, exhaleCount: Int): Int {
    if (inhaleCount == 0 && exhaleCount == 0) return 1
    var rows = maxOf(inhaleCount, exhaleCount, 1)
    while (rows <= maxOf(inhaleCount, exhaleCount) + 6) {
        val inhale = (0 until inhaleCount).map { inhaleSlotAt(it, rows) }
        val exhale = (0 until exhaleCount).map { exhaleSlotAt(it, rows) }
        if (inhale.toSet().size == inhaleCount &&
            exhale.toSet().size == exhaleCount &&
            inhale.toSet().intersect(exhale.toSet()).isEmpty()
        ) {
            return rows
        }
        rows++
    }
    return maxOf(inhaleCount, exhaleCount, 1)
}

private fun inhaleSlotAt(index: Int, rowCount: Int): Pair<Int, Int> {
    val row = (rowCount - 1 - index).coerceAtLeast(0)
    val col = if (index % 2 == 0) 1 else 0
    return row to col
}

private fun exhaleSlotAt(index: Int, rowCount: Int): Pair<Int, Int> {
    val row = index.coerceAtMost(rowCount - 1)
    val col = if (index % 2 == 0) 1 else 0
    return row to col
}

internal fun inhaleGridSlots(count: Int, rowCount: Int): Set<Pair<Int, Int>> =
    (0 until count).map { inhaleSlotAt(it, rowCount) }.toSet()

internal fun exhaleGridSlots(count: Int, rowCount: Int): Set<Pair<Int, Int>> =
    (0 until count).map { exhaleSlotAt(it, rowCount) }.toSet()

fun computeStructureSpec(pattern: BreathingPattern): BreathStructureSpec {
    val inhaleSegments = segmentsForPhase(pattern.inhaleSeconds, SphereRoleKind.InhaleBlue)
    val secondInhale = if (pattern.secondInhaleSeconds > 0f) {
        segmentsForPhase(pattern.secondInhaleSeconds, SphereRoleKind.InhaleBlue)
    } else {
        emptyList()
    }
    val exhaleSegments = segmentsForPhase(pattern.exhaleSeconds, SphereRoleKind.ExhaleRed)

    return BreathStructureSpec(
        inhaleSegments = inhaleSegments + secondInhale,
        exhaleSegments = exhaleSegments,
        topHoldSeconds = pattern.holdAfterInhaleSeconds.takeIf { it > 0f },
        bottomHoldSeconds = pattern.holdAfterExhaleSeconds.takeIf { it > 0f },
    )
}

private fun segmentsForPhase(seconds: Float, role: SphereRoleKind): List<PhaseSegment> {
    if (seconds <= 0f) return emptyList()
    val count = ceil(seconds.toDouble()).toInt()
    return List(count) { index ->
        PhaseSegment(role, min(1f, seconds - index))
    }
}
