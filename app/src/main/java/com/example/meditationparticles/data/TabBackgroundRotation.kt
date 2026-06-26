package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.settings.BackgroundPeriod
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.settings.backgroundPeriodForTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class TabBackgroundRotation(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dayDrawables = listOf(
        R.drawable.day_1,
        R.drawable.day_2,
        R.drawable.day_3,
        R.drawable.day_4,
        R.drawable.day_5,
        R.drawable.day_6,
    )
    private val nightDrawables = listOf(
        R.drawable.night_1,
        R.drawable.night_2,
        R.drawable.night_3,
        R.drawable.night_4,
        R.drawable.night_5,
    )

    private val _currentDrawable = MutableStateFlow(readCurrentDrawable())
    val currentDrawable: StateFlow<Int> = _currentDrawable.asStateFlow()

    fun sync(themeMode: ThemeMode, isSystemDark: Boolean = false) {
        _currentDrawable.value = readCurrentDrawable(themeMode, isSystemDark)
    }

    fun advance(themeMode: ThemeMode, isSystemDark: Boolean = false) {
        val period = resolvePeriod(themeMode, isSystemDark)
        val drawables = drawablesFor(period)
        if (drawables.isEmpty()) {
            _currentDrawable.value = fallbackDrawable
            return
        }

        val indexKey = indexKeyFor(period)
        val currentIndex = prefs.getInt(indexKey, 0).coerceIn(0, drawables.lastIndex)
        _currentDrawable.value = drawables[currentIndex]

        val nextIndex = (currentIndex + 1) % drawables.size
        prefs.edit()
            .putInt(indexKey, nextIndex)
            .putString(KEY_LAST_PERIOD_KIND, period.name)
            .putString(KEY_LAST_PERIOD_DATE, todayDateString())
            .apply()
    }

    private fun readCurrentDrawable(
        themeMode: ThemeMode = ThemeMode.TimeResponsive,
        isSystemDark: Boolean = false,
    ): Int {
        val period = resolvePeriod(themeMode, isSystemDark)
        val drawables = drawablesFor(period)
        if (drawables.isEmpty()) return fallbackDrawable

        val indexKey = indexKeyFor(period)
        val index = prefs.getInt(indexKey, 0).coerceIn(0, drawables.lastIndex)
        return drawables[index]
    }

    private fun resolvePeriod(themeMode: ThemeMode, isSystemDark: Boolean): BackgroundPeriod {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return backgroundPeriodForTheme(themeMode, hour, isSystemDark)
    }

    private fun drawablesFor(period: BackgroundPeriod): List<Int> = when (period) {
        BackgroundPeriod.Daylight -> dayDrawables
        BackgroundPeriod.Nighttime -> nightDrawables
    }

    private fun indexKeyFor(period: BackgroundPeriod): String = when (period) {
        BackgroundPeriod.Daylight -> KEY_DAY_INDEX
        BackgroundPeriod.Nighttime -> KEY_NIGHT_INDEX
    }

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
        private const val KEY_DAY_INDEX = "day_index"
        private const val KEY_NIGHT_INDEX = "night_index"
        private const val KEY_LAST_PERIOD_KIND = "last_period_kind"
        private const val KEY_LAST_PERIOD_DATE = "last_period_date"
        val fallbackDrawable: Int = R.drawable.home_background
    }
}
