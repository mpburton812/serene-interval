package com.safehaven.affirmations.widget

import kotlinx.coroutines.delay

/**
 * Selection feedback for the home-screen mood widget.
 *
 * Glance cannot run Compose animations, so callers apply each frame via widget updates.
 * Timing mirrors in-app [MoodQuickLogCard]: three enlarge→shrink pulses at ~120ms each.
 */
object MoodWidgetSelectionFlash {
    const val CYCLES = 3
    /** Matches in-app tween(120) per enlarge/shrink half-cycle. */
    const val FRAME_DELAY_MS = 120L

    /** Face scale bump (~1.2x+) while bouncing — mimics in-app scale(1.2f). */
    const val NORMAL_CIRCLE_DP = 48
    const val HIGHLIGHT_CIRCLE_DP = 64
    const val NORMAL_ICON_DP = 32
    const val HIGHLIGHT_ICON_DP = 42

    /**
     * One bounce cycle: enlarge at peak color, then shrink with a soft residual tint.
     * Pair is (backgroundAlpha, enlarged).
     */
    val cycleFrames: List<Pair<Float, Boolean>> = listOf(
        1f to true,
        0.35f to false,
    )

    fun isFaceEnlarged(flash: MoodWidgetFlash?, level: Int): Boolean =
        flash != null &&
            flash.level == level &&
            flash.enlarged

    fun circleSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_CIRCLE_DP else NORMAL_CIRCLE_DP

    fun iconSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_ICON_DP else NORMAL_ICON_DP

    fun cornerRadiusDp(enlarged: Boolean): Int = circleSizeDp(enlarged) / 2

    suspend fun run(
        cycles: Int = CYCLES,
        frameDelayMs: Long = FRAME_DELAY_MS,
        frames: List<Pair<Float, Boolean>> = cycleFrames,
        onFrame: suspend (alpha: Float, enlarged: Boolean) -> Unit,
    ) {
        repeat(cycles) {
            for ((alpha, enlarged) in frames) {
                onFrame(alpha, enlarged)
                delay(frameDelayMs)
            }
        }
        onFrame(0f, false)
    }
}
