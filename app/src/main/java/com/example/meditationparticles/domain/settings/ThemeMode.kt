package com.example.meditationparticles.domain.settings

enum class ThemeMode(val label: String) {
    TimeResponsive("Time Responsive"),
    Light("Light"),
    Dark("Dark"),
    System("System"),
}

enum class TimeOfDayPeriod(val label: String) {
    Morning("Morning"),
    Day("Day"),
    Dusk("Dusk"),
    Night("Night"),
}

fun timeOfDayPeriod(hour: Int): TimeOfDayPeriod = when (hour) {
    in 5..10 -> TimeOfDayPeriod.Morning
    in 11..16 -> TimeOfDayPeriod.Day
    in 17..20 -> TimeOfDayPeriod.Dusk
    else -> TimeOfDayPeriod.Night
}
