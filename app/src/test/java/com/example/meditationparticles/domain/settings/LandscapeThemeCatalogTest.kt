package com.example.meditationparticles.domain.settings

import com.example.meditationparticles.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeThemeCatalogTest {
    @Test
    fun spaceDayUsesGoldenSunDrawable() {
        val drawables = LandscapeThemeCatalog.drawablesFor(
            SanctuaryLandscapeThemeId.Space,
            BackgroundPeriod.Daylight,
        )

        assertEquals(listOf(R.drawable.landscape_space_day), drawables)
    }

    @Test
    fun classicThemeUsesLegacyRotationDrawables() {
        val day = LandscapeThemeCatalog.drawablesFor(
            SanctuaryLandscapeThemeId.Classic,
            BackgroundPeriod.Daylight,
        )
        val night = LandscapeThemeCatalog.drawablesFor(
            SanctuaryLandscapeThemeId.Classic,
            BackgroundPeriod.Nighttime,
        )

        assertTrue(day.size >= 2)
        assertTrue(night.size >= 2)
        assertEquals(R.drawable.day_1, day.first())
        assertEquals(R.drawable.night_1, night.first())
    }

    @Test
    fun pickerOptionsExcludeClassic() {
        assertTrue(SanctuaryLandscapeThemeId.pickerOptions.none { it == SanctuaryLandscapeThemeId.Classic })
        assertEquals(7, SanctuaryLandscapeThemeId.pickerOptions.size)
    }
}
