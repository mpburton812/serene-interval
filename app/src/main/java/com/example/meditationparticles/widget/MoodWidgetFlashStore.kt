package com.example.meditationparticles.widget

import java.util.concurrent.ConcurrentHashMap

/** Ephemeral flash overlay shown after a mood is selected on the home-screen widget. */
data class MoodWidgetFlash(
    val colorArgb: Long,
    val alpha: Float,
    val level: Int = 0,
)

object MoodWidgetFlashStore {
    private val flashes = ConcurrentHashMap<Int, MoodWidgetFlash>()

    fun get(appWidgetId: Int): MoodWidgetFlash? = flashes[appWidgetId]

    fun set(appWidgetId: Int, flash: MoodWidgetFlash?) {
        if (flash == null || flash.alpha <= 0f) {
            flashes.remove(appWidgetId)
        } else {
            flashes[appWidgetId] = flash.copy(alpha = flash.alpha.coerceIn(0f, 1f))
        }
    }

    fun clear(appWidgetId: Int) {
        flashes.remove(appWidgetId)
    }

    fun isActive(appWidgetId: Int): Boolean = flashes.containsKey(appWidgetId)
}
