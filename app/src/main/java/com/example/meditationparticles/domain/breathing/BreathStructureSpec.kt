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
        get() = maxOf(
            (inhaleSphereCount + 1) / 2,
            (exhaleSphereCount + 1) / 2,
            1,
        )
}

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
