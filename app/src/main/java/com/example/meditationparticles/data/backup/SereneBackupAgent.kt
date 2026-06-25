package com.example.meditationparticles.data.backup

import android.app.backup.BackupAgentHelper
import android.app.backup.FileBackupHelper
import android.app.backup.SharedPreferencesBackupHelper

/**
 * Includes the Room database in Google device backup only when the user opts in.
 * [backup_rules.xml] still excludes the DB from the default full-backup manifest rules;
 * this agent adds it conditionally at backup time.
 */
class SereneBackupAgent : BackupAgentHelper() {
    override fun onCreate() {
        val snapshot = AutoBackupPreferences(applicationContext).load()
        if (snapshot.cloudBackupEnabled) {
            addHelper(
                DB_HELPER_KEY,
                FileBackupHelper(this, DATABASE_FILE),
            )
        }
        addHelper(
            PREFS_HELPER_KEY,
            SharedPreferencesBackupHelper(
                this,
                AutoBackupPreferences.PREFS_NAME,
                "experience_settings",
                "quick_start_preferences",
            ),
        )
    }

    companion object {
        private const val DB_HELPER_KEY = "serene_db"
        private const val PREFS_HELPER_KEY = "serene_prefs"
        private const val DATABASE_FILE = "../databases/serene_interval.db"
    }
}
