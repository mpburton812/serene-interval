package com.example.meditationparticles.data

import android.content.Context

private const val PREFS_NAME = "affirmation_preferences"
private const val KEY_REMINDER_ENABLED = "reminder_enabled"
private const val KEY_REMINDER_HOUR = "reminder_hour"
private const val KEY_REMINDER_MINUTE = "reminder_minute"
private const val KEY_VIEW_MODE = "view_mode"

class AffirmationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AffirmationPrefsSnapshot = AffirmationPrefsSnapshot(
        reminderEnabled = prefs.getBoolean(KEY_REMINDER_ENABLED, false),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 9),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        viewMode = prefs.getString(KEY_VIEW_MODE, ViewMode.Card.name) ?: ViewMode.Card.name,
    )

    fun save(snapshot: AffirmationPrefsSnapshot) {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, snapshot.reminderEnabled)
            .putInt(KEY_REMINDER_HOUR, snapshot.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, snapshot.reminderMinute)
            .putString(KEY_VIEW_MODE, snapshot.viewMode)
            .apply()
    }

    enum class ViewMode { Card, List }

    data class AffirmationPrefsSnapshot(
        val reminderEnabled: Boolean,
        val reminderHour: Int,
        val reminderMinute: Int,
        val viewMode: String,
    )
}
