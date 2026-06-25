package com.example.meditationparticles.domain.settings

import com.example.meditationparticles.R

object LandscapeThemeCatalog {
    private val classicDayDrawables = listOf(
        R.drawable.day_1,
        R.drawable.day_2,
        R.drawable.day_3,
        R.drawable.day_4,
        R.drawable.day_5,
        R.drawable.day_6,
    )

    private val classicNightDrawables = listOf(
        R.drawable.night_1,
        R.drawable.night_2,
        R.drawable.night_3,
        R.drawable.night_4,
        R.drawable.night_5,
    )

    fun drawablesFor(
        theme: SanctuaryLandscapeThemeId,
        period: BackgroundPeriod,
    ): List<Int> = when (theme) {
        SanctuaryLandscapeThemeId.Classic -> when (period) {
            BackgroundPeriod.Daylight -> classicDayDrawables
            BackgroundPeriod.Nighttime -> classicNightDrawables
        }
        SanctuaryLandscapeThemeId.Beach -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_beach_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_beach_night)
        }
        SanctuaryLandscapeThemeId.Cabin -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_cabin_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_cabin_night)
        }
        SanctuaryLandscapeThemeId.Desert -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_desert_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_desert_night)
        }
        SanctuaryLandscapeThemeId.Snowscape -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_snowscape_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_snowscape_night)
        }
        SanctuaryLandscapeThemeId.DeepWoods -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_deep_woods_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_deep_woods_night)
        }
        SanctuaryLandscapeThemeId.Moon -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_moon_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_moon_night)
        }
        SanctuaryLandscapeThemeId.Space -> when (period) {
            BackgroundPeriod.Daylight -> listOf(R.drawable.landscape_space_day)
            BackgroundPeriod.Nighttime -> listOf(R.drawable.landscape_space_night)
        }
    }

    fun previewDrawable(
        theme: SanctuaryLandscapeThemeId,
        period: BackgroundPeriod,
    ): Int = drawablesFor(theme, period).firstOrNull() ?: R.drawable.home_background
}
