package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.settings.BackgroundPeriod
import com.example.meditationparticles.domain.settings.LandscapeThemeCatalog
import com.example.meditationparticles.domain.settings.SanctuaryLandscapeThemeId
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.settings.backgroundPeriodForTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class TabBackgroundRotation(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentDrawable = MutableStateFlow(readCurrentDrawable())
    val currentDrawable: StateFlow<Int> = _currentDrawable.asStateFlow()

    fun sync(
        landscapeThemeId: SanctuaryLandscapeThemeId,
        themeMode: ThemeMode,
        isSystemDark: Boolean = false,
    ) {
        _currentDrawable.value = readCurrentDrawable(landscapeThemeId, themeMode, isSystemDark)
    }

    fun advance(
        landscapeThemeId: SanctuaryLandscapeThemeId,
        themeMode: ThemeMode,
        isSystemDark: Boolean = false,
    ) {
        val period = resolvePeriod(themeMode, isSystemDark)
        val drawables = drawablesFor(landscapeThemeId, period)
        if (drawables.isEmpty()) {
            _currentDrawable.value = fallbackDrawable
            return
        }

        val indexKey = indexKeyFor(landscapeThemeId, period)
        val currentIndex = prefs.getInt(indexKey, 0).coerceIn(0, drawables.lastIndex)
        _currentDrawable.value = drawables[currentIndex]

        val nextIndex = (currentIndex + 1) % drawables.size
        prefs.edit()
            .putInt(indexKey, nextIndex)
            .putString(KEY_LAST_PERIOD_KIND, period.name)
            .putString(KEY_LAST_LANDSCAPE_THEME, landscapeThemeId.name)
            .putString(KEY_LAST_PERIOD_DATE, todayDateString())
            .apply()
    }

    private fun readCurrentDrawable(
        landscapeThemeId: SanctuaryLandscapeThemeId = SanctuaryLandscapeThemeId.Classic,
        themeMode: ThemeMode = ThemeMode.TimeResponsive,
        isSystemDark: Boolean = false,
    ): Int {
        val period = resolvePeriod(themeMode, isSystemDark)
        val drawables = drawablesFor(landscapeThemeId, period)
        if (drawables.isEmpty()) return fallbackDrawable

        val indexKey = indexKeyFor(landscapeThemeId, period)
        val index = prefs.getInt(indexKey, 0).coerceIn(0, drawables.lastIndex)
        return drawables[index]
    }

    private fun resolvePeriod(themeMode: ThemeMode, isSystemDark: Boolean): BackgroundPeriod {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return backgroundPeriodForTheme(themeMode, hour, isSystemDark)
    }

    private fun drawablesFor(
        landscapeThemeId: SanctuaryLandscapeThemeId,
        period: BackgroundPeriod,
    ): List<Int> = LandscapeThemeCatalog.drawablesFor(landscapeThemeId, period)

    private fun indexKeyFor(
        landscapeThemeId: SanctuaryLandscapeThemeId,
        period: BackgroundPeriod,
    ): String = "index_${landscapeThemeId.name}_${period.name}"

    private fun todayDateString(): String {
        val calendar = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
    }

    companion object {
        private const val PREFS_NAME = "tab_background_rotation"
        private const val KEY_LAST_PERIOD_KIND = "last_period_kind"
        private const val KEY_LAST_LANDSCAPE_THEME = "last_landscape_theme"
        private const val KEY_LAST_PERIOD_DATE = "last_period_date"
        val fallbackDrawable: Int = R.drawable.home_background
    }
}
