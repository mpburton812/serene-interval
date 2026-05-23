package com.example.meditationparticles.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.settings.TimeOfDayPeriod
import com.example.meditationparticles.domain.settings.timeOfDayPeriod

private val MorningPalette = lightColorScheme(
    primary = Color(0xFF6B7A4E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB8C99A),
    onPrimaryContainer = Color(0xFF2A3318),
    secondary = Color(0xFF5A6E80),
    secondaryContainer = Color(0xFFD4E4F2),
    onSecondaryContainer = Color(0xFF3D5260),
    tertiary = Color(0xFF9A6B4A),
    tertiaryContainer = Color(0xFFE8B896),
    background = Color(0xFFFFF8EE),
    onBackground = Color(0xFF1E1C18),
    surface = Color(0xFFFFF8EE),
    onSurface = Color(0xFF1E1C18),
    onSurfaceVariant = Color(0xFF3A3832),
    surfaceContainer = Color(0xFFF5EDD8),
    surfaceContainerHigh = Color(0xFFEFE6D0),
    surfaceContainerLow = Color(0xFFFAF4E8),
    outlineVariant = Color(0xFFC8C0B0),
)

private val DayPalette = lightColorScheme(
    primary = SerenePrimary,
    onPrimary = SereneOnPrimary,
    primaryContainer = SerenePrimaryContainer,
    onPrimaryContainer = SereneOnPrimaryContainer,
    secondary = SereneSecondary,
    secondaryContainer = SereneSecondaryContainer,
    onSecondaryContainer = SereneOnSecondaryContainer,
    tertiary = SereneTertiary,
    tertiaryContainer = SereneTertiaryContainer,
    background = SereneBackground,
    onBackground = SereneOnBackground,
    surface = SereneSurface,
    onSurface = SereneOnSurface,
    onSurfaceVariant = SereneOnSurfaceVariant,
    surfaceContainer = SereneSurfaceContainer,
    surfaceContainerHigh = SereneSurfaceContainerHigh,
    surfaceContainerLow = SereneSurfaceContainerLow,
    outlineVariant = SereneOutlineVariant,
)

private val DuskPalette = lightColorScheme(
    primary = Color(0xFF7A5A48),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4A088),
    onPrimaryContainer = Color(0xFF3D2218),
    secondary = Color(0xFF6A5868),
    secondaryContainer = Color(0xFFE8D0E0),
    onSecondaryContainer = Color(0xFF4A3848),
    tertiary = Color(0xFF8D4D3B),
    tertiaryContainer = Color(0xFFDA8D78),
    background = Color(0xFFFAF0E8),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFAF0E8),
    onSurface = Color(0xFF221A16),
    onSurfaceVariant = Color(0xFF3E342E),
    surfaceContainer = Color(0xFFF0E4DA),
    surfaceContainerHigh = Color(0xFFE8DCD0),
    surfaceContainerLow = Color(0xFFF5EBE2),
    outlineVariant = Color(0xFFC8B8A8),
)

private val NightPalette = darkColorScheme(
    primary = Color(0xFFB0CEB2),
    onPrimary = Color(0xFF1A3820),
    primaryContainer = Color(0xFF334D38),
    onPrimaryContainer = Color(0xFFCCEACE),
    secondary = Color(0xFFB0CADD),
    secondaryContainer = Color(0xFF314A5A),
    onSecondaryContainer = Color(0xFFCCE6FA),
    tertiary = Color(0xFFFFB5A1),
    tertiaryContainer = Color(0xFF703626),
    background = Color(0xFF121816),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF121816),
    onSurface = Color(0xFFE4E2DD),
    onSurfaceVariant = Color(0xFFB8BAB4),
    surfaceContainer = Color(0xFF1E2420),
    surfaceContainerHigh = Color(0xFF282E2A),
    surfaceContainerLow = Color(0xFF181E1A),
    outlineVariant = Color(0xFF424842),
)

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFB0CEB2),
    onPrimary = Color(0xFF233D29),
    primaryContainer = Color(0xFF334D38),
    onPrimaryContainer = Color(0xFFCCEACE),
    secondary = Color(0xFFB0CADD),
    secondaryContainer = Color(0xFF314A5A),
    onSecondaryContainer = Color(0xFFCCE6FA),
    tertiary = Color(0xFFFFB5A1),
    tertiaryContainer = Color(0xFF703626),
    background = Color(0xFF1B1C19),
    onBackground = Color(0xFFE4E2DD),
    surface = Color(0xFF1B1C19),
    onSurface = Color(0xFFE4E2DD),
    onSurfaceVariant = Color(0xFFC2C8C0),
    surfaceContainer = Color(0xFF2A2C28),
    surfaceContainerHigh = Color(0xFF30312E),
    surfaceContainerLow = Color(0xFF242622),
    outlineVariant = Color(0xFF424842),
)

fun resolveColorScheme(
    themeMode: ThemeMode,
    isSystemDark: Boolean,
    hour: Int,
): ColorScheme = when (themeMode) {
    ThemeMode.Light -> DayPalette
    ThemeMode.Dark -> DarkPalette
    ThemeMode.System -> if (isSystemDark) DarkPalette else DayPalette
    ThemeMode.TimeResponsive -> when (timeOfDayPeriod(hour)) {
        TimeOfDayPeriod.Morning -> MorningPalette
        TimeOfDayPeriod.Day -> DayPalette
        TimeOfDayPeriod.Dusk -> DuskPalette
        TimeOfDayPeriod.Night -> NightPalette
    }
}

fun isDarkScheme(scheme: ColorScheme): Boolean =
    scheme.background.red + scheme.background.green + scheme.background.blue < 1.2f
