package com.safehaven.affirmations.widget

import java.util.concurrent.ConcurrentHashMap

/** Ephemeral flash overlay shown after a mood is selected on the home-screen widget. */
data class MoodWidgetFlash(
    val colorArgb: Long,
    val alpha: Float,
    val level: Int = 0,
    val enlarged: Boolean = false,
    val startedAtMillis: Long = System.currentTimeMillis(),
)

object MoodWidgetFlashStore {
    private val flashes = ConcurrentHashMap<Int, MoodWidgetFlash>()

    /** Stale in-memory flashes (e.g. after a killed update) must not block new logs forever. */
    const val ACTIVE_TIMEOUT_MS = 2_500L

    fun get(appWidgetId: Int): MoodWidgetFlash? {
        val flash = flashes[appWidgetId] ?: return null
        if (isStale(flash)) {
            flashes.remove(appWidgetId)
            return null
        }
        return flash
    }

    fun set(appWidgetId: Int, flash: MoodWidgetFlash?) {
        if (flash == null || (flash.alpha <= 0f && !flash.enlarged)) {
            flashes.remove(appWidgetId)
        } else {
            flashes[appWidgetId] = flash.copy(alpha = flash.alpha.coerceIn(0f, 1f))
        }
    }

    fun clear(appWidgetId: Int) {
        flashes.remove(appWidgetId)
    }

    fun isActive(appWidgetId: Int): Boolean {
        val flash = flashes[appWidgetId] ?: return false
        if (isStale(flash)) {
            flashes.remove(appWidgetId)
            return false
        }
        return true
    }

    private fun isStale(flash: MoodWidgetFlash): Boolean =
        System.currentTimeMillis() - flash.startedAtMillis > ACTIVE_TIMEOUT_MS
}
