package com.safehaven.affirmations.widget

import kotlinx.coroutines.delay

/**
 * Bounce + background flash for the mood widget. Glance cannot run Compose animations, so callers
 * apply each frame via widget updates.
 */
object MoodWidgetSelectionFlash {
    const val CYCLES = 3
    /** Matches in-app [androidx.compose.animation.core.tween] duration (120 ms per half-cycle). */
    const val BOUNCE_HALF_CYCLE_MS = 120L

    /** Face scale bump (~1.2x) while enlarged — mimics in-app bounce. */
    const val NORMAL_CIRCLE_DP = 48
    const val HIGHLIGHT_CIRCLE_DP = 58
    const val NORMAL_ICON_DP = 32
    const val HIGHLIGHT_ICON_DP = 38

    data class Frame(
        val backgroundAlpha: Float,
        val faceEnlarged: Boolean,
    )

    /** Three up/down bounce cycles, then a clear frame. */
    val bounceFrames: List<Frame> = buildList {
        repeat(CYCLES) {
            add(Frame(backgroundAlpha = 0.7f, faceEnlarged = true))
            add(Frame(backgroundAlpha = 0.2f, faceEnlarged = false))
        }
        add(Frame(backgroundAlpha = 0f, faceEnlarged = false))
    }

    fun isFaceEnlarged(flash: MoodWidgetFlash?, level: Int): Boolean =
        flash != null && flash.level == level && flash.faceEnlarged

    fun circleSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_CIRCLE_DP else NORMAL_CIRCLE_DP

    fun iconSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_ICON_DP else NORMAL_ICON_DP

    fun cornerRadiusDp(enlarged: Boolean): Int = circleSizeDp(enlarged) / 2

    suspend fun run(
        frames: List<Frame> = bounceFrames,
        halfCycleMs: Long = BOUNCE_HALF_CYCLE_MS,
        onFrame: suspend (Frame) -> Unit,
    ) {
        frames.forEach { frame ->
            onFrame(frame)
            if (frame != frames.last()) {
                delay(halfCycleMs)
            }
        }
    }
}
