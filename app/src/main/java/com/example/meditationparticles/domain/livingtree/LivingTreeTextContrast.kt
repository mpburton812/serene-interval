package com.example.meditationparticles.domain.livingtree

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

object LivingTreeTextContrast {
    data class LabelColors(
        val text: Color,
        val outline: Color,
        val plateFill: Color,
        val plateBorder: Color,
    )

    fun labelColors(scheme: ColorScheme, isDark: Boolean): LabelColors {
        val text = scheme.onSurface.copy(alpha = 0.94f)
        val outline = if (isDark) {
            Color.Black.copy(alpha = 0.55f)
        } else {
            Color.White.copy(alpha = 0.72f)
        }
        val plateFill = if (isDark) {
            scheme.surfaceContainerHigh.copy(alpha = 0.78f)
        } else {
            scheme.surfaceContainerLow.copy(alpha = 0.90f)
        }
        val plateBorder = scheme.outlineVariant.copy(alpha = if (isDark) 0.42f else 0.58f)
        return LabelColors(text, outline, plateFill, plateBorder)
    }

    fun textOnBubbleFill(
        bubbleColors: List<Color>,
        scheme: ColorScheme,
        isDark: Boolean,
    ): Color {
        if (bubbleColors.isEmpty()) {
            return if (isDark) Color.White.copy(alpha = 0.95f) else scheme.onSurface.copy(alpha = 0.92f)
        }
        val avgLuminance = bubbleColors.map(::relativeLuminance).average().toFloat()
        return if (avgLuminance > 0.45f) {
            Color(0xFF1A1816).copy(alpha = 0.94f)
        } else {
            Color.White.copy(alpha = 0.95f)
        }
    }

    fun relativeLuminance(color: Color): Float {
        fun channel(value: Float): Float =
            if (value <= 0.03928f) {
                value / 12.92f
            } else {
                ((value + 0.055f) / 1.055f).pow(2.4f)
            }

        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
}
