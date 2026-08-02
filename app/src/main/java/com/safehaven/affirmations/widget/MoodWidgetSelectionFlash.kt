package com.safehaven.affirmations.widget

import kotlinx.coroutines.delay

/**
 * Fast fade-in / fade-out of the selected mood color across the whole widget background.
 * Glance cannot run Compose animations, so callers apply each alpha frame via widget updates.
 */
object MoodWidgetSelectionFlash {
    const val CYCLES = 3
    const val FRAME_DELAY_MS = 45L

    /** Face scale bump (~1.2x) while flash alpha is near peak — mimics in-app bounce. */
    const val NORMAL_CIRCLE_DP = 48
    const val HIGHLIGHT_CIRCLE_DP = 58
    const val NORMAL_ICON_DP = 32
    const val HIGHLIGHT_ICON_DP = 38
    const val FACE_ENLARGE_ALPHA_THRESHOLD = 0.7f

    /** One fade-in then fade-out cycle (selected color alpha). */
    val cycleAlphas: List<Float> = listOf(0.35f, 0.7f, 1f, 0.7f, 0.35f, 0f)

    fun isFaceEnlarged(flash: MoodWidgetFlash?, level: Int): Boolean =
        flash != null &&
            flash.level == level &&
            flash.alpha >= FACE_ENLARGE_ALPHA_THRESHOLD

    fun circleSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_CIRCLE_DP else NORMAL_CIRCLE_DP

    fun iconSizeDp(enlarged: Boolean): Int =
        if (enlarged) HIGHLIGHT_ICON_DP else NORMAL_ICON_DP

    fun cornerRadiusDp(enlarged: Boolean): Int = circleSizeDp(enlarged) / 2

    suspend fun run(
        cycles: Int = CYCLES,
        frameDelayMs: Long = FRAME_DELAY_MS,
        alphas: List<Float> = cycleAlphas,
        onFrame: suspend (alpha: Float) -> Unit,
    ) {
        repeat(cycles) {
            for (alpha in alphas) {
                onFrame(alpha)
                delay(frameDelayMs)
            }
        }
        onFrame(0f)
    }
}
