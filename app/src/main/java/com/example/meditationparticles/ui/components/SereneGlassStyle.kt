package com.example.meditationparticles.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.meditationparticles.ui.theme.isDarkScheme

@Immutable
data class SereneGlassStyle(
    val fill: Color,
    val border: Color,
)

fun sereneGlassStyle(scheme: ColorScheme): SereneGlassStyle {
    val isDark = isDarkScheme(scheme)
    return SereneGlassStyle(
        fill = if (isDark) {
            scheme.surfaceContainerHigh.copy(alpha = 0.72f)
        } else {
            scheme.surfaceContainerLow.copy(alpha = 0.92f)
        },
        border = if (isDark) {
            scheme.outlineVariant.copy(alpha = 0.45f)
        } else {
            scheme.outlineVariant.copy(alpha = 0.65f)
        },
    )
}
