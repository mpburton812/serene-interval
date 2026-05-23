package com.example.meditationparticles.ui.theme

import androidx.compose.ui.graphics.Color

// Serene Interval palette — from DESIGN.md
val SereneBackground = Color(0xFFFBF9F4)
val SereneOnBackground = Color(0xFF1B1C19)
val SereneSurface = Color(0xFFFBF9F4)
val SereneOnSurface = Color(0xFF1B1C19)
val SereneOnSurfaceVariant = Color(0xFF424842)
val SerenePrimary = Color(0xFF4A654E)
val SereneOnPrimary = Color(0xFFFFFFFF)
val SerenePrimaryContainer = Color(0xFF8BA88E)
val SereneOnPrimaryContainer = Color(0xFF233D29)
val SereneSecondary = Color(0xFF496172)
val SereneSecondaryContainer = Color(0xFFC9E3F7)
val SereneOnSecondaryContainer = Color(0xFF4D6677)
val SereneTertiary = Color(0xFF8D4D3B)
val SereneTertiaryContainer = Color(0xFFDA8D78)
val SereneSurfaceContainer = Color(0xFFF0EEE9)
val SereneSurfaceContainerHigh = Color(0xFFEAE8E3)
val SereneSurfaceContainerLow = Color(0xFFF5F3EE)
val SereneOutlineVariant = Color(0xFFC2C8C0)

// Breathing sand colors — blue inhale, red exhale, purple hold
val BreathSandInhale = Color(0xFF6EC4FF)
val BreathSandExhale = Color(0xFFE86B6B)
val BreathSandHold = Color(0xFFBF6BFF)
val BreathStartStar = Color(0xFFFFC107)

val PipeMetal = Color(0xFF4A4038)

val GlassFill = Color(0x66FBF9F4)
val GlassBorder = Color(0x33FFFFFF)
val GlassTube = Color(0xCCE4E2DD)

// Legacy demo background
val SandBackground = Color(0xFF1A1612)

/** Maps slider position 0..1 to red (0°) through violet (~300°). */
fun spectrumColor(position: Float, alpha: Float = 1f): Color {
    val hue = position.coerceIn(0f, 1f) * 300f
    return Color.hsv(hue, 0.85f, 0.95f, alpha)
}

val spectrumGradientColors = listOf(
    Color.hsv(0f, 0.9f, 0.95f),
    Color.hsv(60f, 0.9f, 0.95f),
    Color.hsv(120f, 0.9f, 0.95f),
    Color.hsv(180f, 0.9f, 0.95f),
    Color.hsv(240f, 0.9f, 0.95f),
    Color.hsv(300f, 0.9f, 0.95f),
)
