package com.example.meditationparticles.domain.settings

enum class BackgroundPeriod {
    Daylight,
    Nighttime,
}

fun backgroundPeriod(hour: Int): BackgroundPeriod = when (timeOfDayPeriod(hour)) {
    TimeOfDayPeriod.Night -> BackgroundPeriod.Nighttime
    TimeOfDayPeriod.Morning,
    TimeOfDayPeriod.Day,
    TimeOfDayPeriod.Dusk,
    -> BackgroundPeriod.Daylight
}

fun backgroundPeriodForTheme(
    themeMode: ThemeMode,
    hour: Int,
    isSystemDark: Boolean = false,
): BackgroundPeriod = when (themeMode) {
    ThemeMode.Light -> BackgroundPeriod.Daylight
    ThemeMode.Dark -> BackgroundPeriod.Nighttime
    ThemeMode.System -> if (isSystemDark) BackgroundPeriod.Nighttime else BackgroundPeriod.Daylight
    ThemeMode.TimeResponsive -> backgroundPeriod(hour)
}
