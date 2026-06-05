package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerPresets
import com.example.meditationparticles.domain.timer.TimerSoundOption

class TimerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TimerPrefsSnapshot {
        val modeName = prefs.getString(KEY_DISPLAY_MODE, TimerDisplayMode.Hourglass.name)
        val displayMode = runCatching {
            TimerDisplayMode.valueOf(modeName ?: TimerDisplayMode.Hourglass.name)
        }.getOrDefault(TimerDisplayMode.Hourglass)

        val soundName = prefs.getString(KEY_SOUND, TimerSoundOption.None.name)
        val sound = TimerSoundOption.fromStoredName(soundName)

        val bellSound = TimerBellSoundChoice.fromStoredName(
            prefs.getString(KEY_BELL_SOUND, TimerBellSoundChoice.Default.name),
        )

        return TimerPrefsSnapshot(
            displayMode = displayMode,
            targetMinutes = prefs.getInt(KEY_TARGET_MINUTES, TimerPresets.DEFAULT_MINUTES),
            sound = sound,
            bellSound = bellSound,
            bellSystemUri = prefs.getString(KEY_BELL_SOUND_URI, null),
            reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
            reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 8),
            reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        )
    }

    fun save(snapshot: TimerPrefsSnapshot) {
        val editor = prefs.edit()
            .putString(KEY_DISPLAY_MODE, snapshot.displayMode.name)
            .putInt(KEY_TARGET_MINUTES, snapshot.targetMinutes)
            .putString(KEY_SOUND, snapshot.sound.name)
            .putString(KEY_BELL_SOUND, snapshot.bellSound.name)
        if (snapshot.bellSound == TimerBellSoundChoice.SystemUri && snapshot.bellSystemUri != null) {
            editor.putString(KEY_BELL_SOUND_URI, snapshot.bellSystemUri)
        } else {
            editor.remove(KEY_BELL_SOUND_URI)
        }
        editor
            .putBoolean(KEY_REMINDER_ENABLED, snapshot.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, snapshot.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, snapshot.reminderMinute)
            .apply()
    }

    data class TimerPrefsSnapshot(
        val displayMode: TimerDisplayMode,
        val targetMinutes: Int,
        val sound: TimerSoundOption,
        val bellSound: TimerBellSoundChoice = TimerBellSoundChoice.Default,
        val bellSystemUri: String? = null,
        val reminderEnabled: Boolean,
        val reminderHour: Int,
        val reminderMinute: Int,
    )

    companion object {
        private const val PREFS_NAME = "timer_preferences"
        private const val KEY_DISPLAY_MODE = "display_mode"
        private const val KEY_TARGET_MINUTES = "target_minutes"
        private const val KEY_SOUND = "sound"
        private const val KEY_BELL_SOUND = "bell_sound"
        private const val KEY_BELL_SOUND_URI = "bell_sound_uri"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}
