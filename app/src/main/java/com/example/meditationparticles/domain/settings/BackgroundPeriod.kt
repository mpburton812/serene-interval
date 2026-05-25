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
