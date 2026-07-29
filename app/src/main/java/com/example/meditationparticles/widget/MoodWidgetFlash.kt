package com.example.meditationparticles.widget

import androidx.compose.ui.graphics.Color
import com.example.meditationparticles.domain.mood.MoodScale

data class MoodWidgetFlashState(
    val level: Int,
    val blend: Float,
) {
    init {
        require(blend in 0f..1f) { "blend must be between 0 and 1" }
    }
}

object MoodWidgetFlash {
    const val PULSE_COUNT = 3
    const val HALF_PULSE_MS = 120L
    private const val FADE_STEPS = 4

    fun fadeInSteps(): List<Float> = (0..FADE_STEPS).map { step ->
        step.toFloat() / FADE_STEPS
    }

    fun fadeOutSteps(): List<Float> = (FADE_STEPS - 1 downTo 0).map { step ->
        step.toFloat() / FADE_STEPS
    }

    fun stepDelayMs(): Long = HALF_PULSE_MS / fadeInSteps().size

    fun blendedBackground(
        baseBackground: Color,
        moodLevel: Int,
        blend: Float,
    ): Color {
        val moodColor = Color(MoodScale.colorArgb(moodLevel)).copy(alpha = 1f)
        val clampedBlend = blend.coerceIn(0f, 1f)
        return Color(
            red = baseBackground.red + (moodColor.red - baseBackground.red) * clampedBlend,
            green = baseBackground.green + (moodColor.green - baseBackground.green) * clampedBlend,
            blue = baseBackground.blue + (moodColor.blue - baseBackground.blue) * clampedBlend,
            alpha = baseBackground.alpha + (1f - baseBackground.alpha) * clampedBlend,
        )
    }
}
