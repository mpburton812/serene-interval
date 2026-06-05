package com.example.meditationparticles.data

import android.content.Context

class MoodTrackerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBackfillDone(): Boolean = prefs.getBoolean(KEY_BACKFILL_DONE, false)

    fun setBackfillDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_BACKFILL_DONE, done).apply()
    }

    companion object {
        private const val PREFS_NAME = "mood_tracker"
        private const val KEY_BACKFILL_DONE = "moodTrackerBackfillDone"
    }
}
