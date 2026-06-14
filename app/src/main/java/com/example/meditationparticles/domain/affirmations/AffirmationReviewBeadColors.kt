package com.example.meditationparticles.domain.affirmations

import androidx.compose.ui.graphics.Color

object AffirmationReviewBeadColors {
    /** Red (left / first bead). */
    private const val HUE_RED = 0f

    /** Purple (right / last bead). */
    private const val HUE_PURPLE = 275f

    private const val SATURATION = 0.78f
    private const val VALUE = 0.92f

    fun colorForIndex(index: Int, total: Int): Color {
        if (total <= 0) return Color.hsv(HUE_RED, SATURATION, VALUE)
        val hue = if (total == 1) {
            HUE_RED
        } else {
            HUE_RED + (HUE_PURPLE - HUE_RED) * index.toFloat() / (total - 1).toFloat()
        }
        return Color.hsv(hue, SATURATION, VALUE)
    }
}
