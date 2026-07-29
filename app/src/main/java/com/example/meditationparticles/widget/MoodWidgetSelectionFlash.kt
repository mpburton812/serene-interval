package com.example.meditationparticles.widget

import kotlinx.coroutines.delay

/**
 * Fast fade-in / fade-out of the selected mood color across the whole widget background.
 * Glance cannot run Compose animations, so callers apply each alpha frame via widget updates.
 */
object MoodWidgetSelectionFlash {
    const val CYCLES = 3
    const val FRAME_DELAY_MS = 45L

    /** One fade-in then fade-out cycle (selected color alpha). */
    val cycleAlphas: List<Float> = listOf(0.35f, 0.7f, 1f, 0.7f, 0.35f, 0f)

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
