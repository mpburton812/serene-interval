package com.example.meditationparticles.data.backup

import android.content.Context
import com.example.meditationparticles.domain.backup.AutoBackupFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AutoBackupSnapshot(
    val autoBackupEnabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.Weekly,
    val destinationTreeUri: String? = null,
    val cloudBackupEnabled: Boolean = false,
    val lastBackupAtMillis: Long? = null,
    val lastBackupMessage: String? = null,
    val backupPromptDismissed: Boolean = false,
    val backupPromptShown: Boolean = false,
) {
    val isConfigured: Boolean get() = !destinationTreeUri.isNullOrBlank()
}

class AutoBackupPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<AutoBackupSnapshot> = _snapshot.asStateFlow()

    fun load(): AutoBackupSnapshot = AutoBackupSnapshot(
        autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false),
        frequency = AutoBackupFrequency.fromStored(prefs.getString(KEY_FREQUENCY, null)),
        destinationTreeUri = prefs.getString(KEY_DESTINATION_TREE_URI, null),
        cloudBackupEnabled = prefs.getBoolean(KEY_CLOUD_BACKUP_ENABLED, false),
        lastBackupAtMillis = prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L },
        lastBackupMessage = prefs.getString(KEY_LAST_BACKUP_MESSAGE, null),
        backupPromptDismissed = prefs.getBoolean(KEY_BACKUP_PROMPT_DISMISSED, false),
        backupPromptShown = prefs.getBoolean(KEY_BACKUP_PROMPT_SHOWN, false),
    )

    fun save(snapshot: AutoBackupSnapshot) {
        prefs.edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, snapshot.autoBackupEnabled)
            .putString(KEY_FREQUENCY, snapshot.frequency.name)
            .putString(KEY_DESTINATION_TREE_URI, snapshot.destinationTreeUri)
            .putBoolean(KEY_CLOUD_BACKUP_ENABLED, snapshot.cloudBackupEnabled)
            .putLong(KEY_LAST_BACKUP_AT, snapshot.lastBackupAtMillis ?: 0L)
            .putString(KEY_LAST_BACKUP_MESSAGE, snapshot.lastBackupMessage)
            .putBoolean(KEY_BACKUP_PROMPT_DISMISSED, snapshot.backupPromptDismissed)
            .putBoolean(KEY_BACKUP_PROMPT_SHOWN, snapshot.backupPromptShown)
            .apply()
        _snapshot.value = snapshot
    }

    fun update(transform: (AutoBackupSnapshot) -> AutoBackupSnapshot) {
        save(transform(load()))
    }

    fun markBackupPromptShown() {
        update { it.copy(backupPromptShown = true) }
    }

    fun dismissBackupPromptPermanently() {
        update { it.copy(backupPromptDismissed = true, backupPromptShown = true) }
    }

    fun recordBackupResult(atMillis: Long, message: String) {
        update {
            it.copy(
                lastBackupAtMillis = atMillis,
                lastBackupMessage = message,
            )
        }
    }

    companion object {
        const val PREFS_NAME = "auto_backup_preferences"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_DESTINATION_TREE_URI = "destination_tree_uri"
        private const val KEY_CLOUD_BACKUP_ENABLED = "cloud_backup_enabled"
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_LAST_BACKUP_MESSAGE = "last_backup_message"
        private const val KEY_BACKUP_PROMPT_DISMISSED = "backup_prompt_dismissed"
        private const val KEY_BACKUP_PROMPT_SHOWN = "backup_prompt_shown"
    }
}
