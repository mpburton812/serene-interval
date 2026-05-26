package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.breathing.BreathingVisualMode

class BreathingDisplayPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadVisualMode(): BreathingVisualMode {
        val name = prefs.getString(KEY_VISUAL_MODE, BreathingVisualMode.A.name)
        return runCatching {
            BreathingVisualMode.valueOf(name ?: BreathingVisualMode.A.name)
        }.getOrDefault(BreathingVisualMode.A)
    }

    fun saveVisualMode(mode: BreathingVisualMode) {
        prefs.edit()
            .putString(KEY_VISUAL_MODE, mode.name)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "breathing_display_preferences"
        private const val KEY_VISUAL_MODE = "visual_mode"
    }
}
