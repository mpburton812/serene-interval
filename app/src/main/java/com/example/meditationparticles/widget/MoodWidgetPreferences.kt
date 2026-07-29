package com.example.meditationparticles.widget

import android.content.Context
import android.content.SharedPreferences

enum class MoodWidgetBackgroundStyle {
    WHITE,
    BLACK,
    ;

    companion object {
        fun fromStored(value: String?): MoodWidgetBackgroundStyle =
            entries.find { it.name == value } ?: WHITE
    }
}

data class MoodWidgetConfig(
    val backgroundStyle: MoodWidgetBackgroundStyle = MoodWidgetBackgroundStyle.WHITE,
    val transparency: Float = DEFAULT_TRANSPARENCY,
) {
    init {
        require(transparency in 0f..1f) { "transparency must be between 0 and 1" }
    }

    companion object {
        const val DEFAULT_TRANSPARENCY = 0.85f
    }
}

class MoodWidgetPreferences internal constructor(
    private val prefs: SharedPreferences,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    fun load(appWidgetId: Int): MoodWidgetConfig {
        val style = MoodWidgetBackgroundStyle.fromStored(
            prefs.getString(keyBackgroundStyle(appWidgetId), null),
        )
        val transparency = prefs.getFloat(
            keyTransparency(appWidgetId),
            MoodWidgetConfig.DEFAULT_TRANSPARENCY,
        ).coerceIn(0f, 1f)
        return MoodWidgetConfig(backgroundStyle = style, transparency = transparency)
    }

    fun save(appWidgetId: Int, config: MoodWidgetConfig) {
        prefs.edit()
            .putString(keyBackgroundStyle(appWidgetId), config.backgroundStyle.name)
            .putFloat(keyTransparency(appWidgetId), config.transparency.coerceIn(0f, 1f))
            .apply()
    }

    fun loadFlash(appWidgetId: Int): MoodWidgetFlashState? {
        val level = prefs.getInt(keyFlashLevel(appWidgetId), NO_FLASH_LEVEL)
        if (level == NO_FLASH_LEVEL) return null
        val blend = prefs.getFloat(keyFlashBlend(appWidgetId), 0f).coerceIn(0f, 1f)
        return MoodWidgetFlashState(level = level, blend = blend)
    }

    fun saveFlash(appWidgetId: Int, flash: MoodWidgetFlashState) {
        prefs.edit()
            .putInt(keyFlashLevel(appWidgetId), flash.level)
            .putFloat(keyFlashBlend(appWidgetId), flash.blend.coerceIn(0f, 1f))
            .apply()
    }

    fun clearFlash(appWidgetId: Int) {
        prefs.edit()
            .remove(keyFlashLevel(appWidgetId))
            .remove(keyFlashBlend(appWidgetId))
            .apply()
    }

    fun remove(appWidgetId: Int) {
        prefs.edit()
            .remove(keyBackgroundStyle(appWidgetId))
            .remove(keyTransparency(appWidgetId))
            .remove(keyFlashLevel(appWidgetId))
            .remove(keyFlashBlend(appWidgetId))
            .apply()
    }

    companion object {
        const val PREFS_NAME = "mood_widget"
        internal const val NO_FLASH_LEVEL = -1

        internal fun keyBackgroundStyle(appWidgetId: Int): String =
            "background_style_$appWidgetId"

        internal fun keyTransparency(appWidgetId: Int): String =
            "transparency_$appWidgetId"

        internal fun keyFlashLevel(appWidgetId: Int): String =
            "flash_level_$appWidgetId"

        internal fun keyFlashBlend(appWidgetId: Int): String =
            "flash_blend_$appWidgetId"
    }
}
