package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import kotlin.math.min

enum class SphereRole {
    HoldPurple,
    InhaleBlue,
    ExhaleRed,
}

data class BreathSphere(
    val id: Int,
    val center: Offset,
    val radius: Float,
    val role: SphereRole,
)

data class BreathStructureLayout(
    val topHold: BreathSphere,
    val bottomHold: BreathSphere,
    val pairs: List<Pair<BreathSphere, BreathSphere>>,
    val scale: Float,
) {
    val allSpheres: List<BreathSphere> =
        listOf(topHold, bottomHold) + pairs.flatMap { listOf(it.first, it.second) }

    val inhalePath: List<BreathSphere> = listOf(
        pairs[3].second,
        pairs[2].first,
        pairs[1].second,
        pairs[0].first,
    )

    val exhalePath: List<BreathSphere> = listOf(
        pairs[0].second,
        pairs[1].first,
        pairs[2].second,
        pairs[3].first,
    )
}

data class SphereVisualState(
    val fillLevel: Float,
    val isActive: Boolean,
)

private const val FILL_SECONDS = 1f
private const val HOLD_TO_SMALL_RATIO = 1.1f
private const val MIN_SPHERE_GAP = 10f
private const val ZONE_PADDING = 12f

fun computeStructureLayout(
    width: Float,
    height: Float,
    topInset: Float = 0f,
    bottomInset: Float = 0f,
): BreathStructureLayout {
    val zoneTop = topInset + ZONE_PADDING
    val zoneBottom = height - bottomInset - ZONE_PADDING
    val zoneHeight = (zoneBottom - zoneTop).coerceAtLeast(80f)
    val scale = min(width, height) / 900f
    val gap = MIN_SPHERE_GAP

    // Total vertical span: 2×hold + 8×small + 5×gap  (holdR = ratio × smallR)
    val heightDenom = 8f + 2f * HOLD_TO_SMALL_RATIO
    val smallRFromHeight = ((zoneHeight - 5f * gap) / heightDenom).coerceAtLeast(8f)

    // Pick column spread, then clamp radius to what fits horizontally.
    var columnOffset = (width * 0.27f).coerceIn(48f, width * 0.36f)
    var smallRFromWidth = width / 2f - ZONE_PADDING - columnOffset
    if (smallRFromWidth < smallRFromHeight) {
        columnOffset = (width / 2f - ZONE_PADDING - smallRFromHeight).coerceAtLeast(48f)
        smallRFromWidth = width / 2f - ZONE_PADDING - columnOffset
    }

    val smallR = min(smallRFromHeight, smallRFromWidth).coerceAtLeast(8f)
    val holdR = (smallR * HOLD_TO_SMALL_RATIO).coerceAtMost(width / 2f - ZONE_PADDING)

    // Place six row centers so content is vertically centered in the zone.
    val contentSpan = 2f * holdR + 8f * smallR + 5f * gap
    var y = zoneTop + (zoneHeight - contentSpan) / 2f + holdR

    val rowYs = buildList {
        add(y) // top hold
        y += holdR + gap + smallR
        repeat(3) {
            add(y)
            y += 2f * smallR + gap
        }
        add(y) // 4th pair row
        y += smallR + gap + holdR
        add(y) // bottom hold
    }

    val centerX = width / 2f
    val leftX = centerX - columnOffset
    val rightX = centerX + columnOffset

    fun pair(level: Int, leftRole: SphereRole, rightRole: SphereRole): Pair<BreathSphere, BreathSphere> {
        val y = rowYs[level + 1]
        val baseId = 2 + level * 2
        return BreathSphere(baseId, Offset(leftX, y), smallR, leftRole) to
            BreathSphere(baseId + 1, Offset(rightX, y), smallR, rightRole)
    }

    val pairs = listOf(
        pair(0, SphereRole.InhaleBlue, SphereRole.ExhaleRed),
        pair(1, SphereRole.ExhaleRed, SphereRole.InhaleBlue),
        pair(2, SphereRole.InhaleBlue, SphereRole.ExhaleRed),
        pair(3, SphereRole.ExhaleRed, SphereRole.InhaleBlue),
    )

    return BreathStructureLayout(
        topHold = BreathSphere(0, Offset(centerX, rowYs[0]), holdR, SphereRole.HoldPurple),
        bottomHold = BreathSphere(1, Offset(centerX, rowYs[5]), holdR, SphereRole.HoldPurple),
        pairs = pairs,
        scale = scale,
    )
}

fun computeSphereVisuals(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
): Map<Int, SphereVisualState> {
    val fills = layout.allSpheres.associate { it.id to SphereVisualState(0f, false) }.toMutableMap()

    when (sessionState.phase) {
        BreathPhase.Inhale, BreathPhase.SecondInhale -> {
            applyPathFill(
                fills = fills,
                path = layout.inhalePath,
                phaseProgress = sessionState.phaseProgress,
                phaseDurationSec = sessionState.phaseDurationSeconds,
            )
        }
        BreathPhase.HoldIn -> {
            layout.inhalePath.forEach { fills[it.id] = SphereVisualState(1f, false) }
            fills[layout.topHold.id] = holdFill(sessionState.phaseProgress, sessionState.phaseDurationSeconds, true)
        }
        BreathPhase.Exhale -> {
            layout.inhalePath.forEach { fills[it.id] = SphereVisualState(1f, false) }
            fills[layout.topHold.id] = SphereVisualState(1f, false)
            applyPathFill(
                fills = fills,
                path = layout.exhalePath,
                phaseProgress = sessionState.phaseProgress,
                phaseDurationSec = sessionState.phaseDurationSeconds,
            )
        }
        BreathPhase.HoldOut -> {
            layout.inhalePath.forEach { fills[it.id] = SphereVisualState(1f, false) }
            layout.exhalePath.forEach { fills[it.id] = SphereVisualState(1f, false) }
            fills[layout.topHold.id] = SphereVisualState(1f, false)
            fills[layout.bottomHold.id] = holdFill(sessionState.phaseProgress, sessionState.phaseDurationSeconds, true)
        }
        else -> { /* translucent */ }
    }

    return fills
}

private fun applyPathFill(
    fills: MutableMap<Int, SphereVisualState>,
    path: List<BreathSphere>,
    phaseProgress: Float,
    phaseDurationSec: Float,
) {
    if (path.isEmpty()) return
    val segmentSpan = 1f / path.size
    val segmentDuration = (phaseDurationSec / path.size).coerceAtLeast(0.01f)
    val overall = phaseProgress.coerceIn(0f, 1f)
    val activeIndex = (overall / segmentSpan).toInt().coerceIn(0, path.lastIndex)
    val localT = ((overall - activeIndex * segmentSpan) / segmentSpan).coerceIn(0f, 1f)
    val activeFill = min(1f, (localT * segmentDuration) / FILL_SECONDS)

    path.forEachIndexed { index, sphere ->
        when {
            index < activeIndex -> fills[sphere.id] = SphereVisualState(1f, false)
            index == activeIndex -> fills[sphere.id] = SphereVisualState(activeFill, true)
            else -> fills[sphere.id] = SphereVisualState(0f, false)
        }
    }
}

private fun holdFill(
    phaseProgress: Float,
    phaseDurationSec: Float,
    isActive: Boolean,
): SphereVisualState {
    val fill = min(1f, (phaseProgress * phaseDurationSec) / FILL_SECONDS)
    return SphereVisualState(fill, isActive && fill < 1f)
}

fun activeMoteSphere(
    sessionState: BreathingSessionState,
    layout: BreathStructureLayout,
    visuals: Map<Int, SphereVisualState>,
): BreathSphere? {
    return when (sessionState.phase) {
        BreathPhase.Inhale, BreathPhase.SecondInhale -> {
            layout.inhalePath.firstOrNull { visuals[it.id]?.isActive == true }
        }
        BreathPhase.HoldIn -> {
            if ((visuals[layout.topHold.id]?.fillLevel ?: 0f) < 1f) layout.topHold else null
        }
        BreathPhase.Exhale -> {
            layout.exhalePath.firstOrNull { visuals[it.id]?.isActive == true }
        }
        BreathPhase.HoldOut -> {
            if ((visuals[layout.bottomHold.id]?.fillLevel ?: 0f) < 1f) layout.bottomHold else null
        }
        else -> null
    }
}
