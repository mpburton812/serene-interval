package com.example.meditationparticles.data.backup

import android.content.Context
import com.example.meditationparticles.domain.backup.AutoBackupFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AutoBackupSnapshot(
    val autoBackupEnabled: Boolean = false,
    val frequency: AutoBackupFrequency = AutoBackupFrequency.Weekly,
    val destinationTreeUri: String? = null,
    val lastBackupAtMillis: Long? = null,
    val lastBackupMessage: String? = null,
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
        lastBackupAtMillis = prefs.getLong(KEY_LAST_BACKUP_AT, 0L).takeIf { it > 0L },
        lastBackupMessage = prefs.getString(KEY_LAST_BACKUP_MESSAGE, null),
    )

    fun save(snapshot: AutoBackupSnapshot) {
        prefs.edit()
            .putBoolean(KEY_AUTO_BACKUP_ENABLED, snapshot.autoBackupEnabled)
            .putString(KEY_FREQUENCY, snapshot.frequency.name)
            .putString(KEY_DESTINATION_TREE_URI, snapshot.destinationTreeUri)
            .putLong(KEY_LAST_BACKUP_AT, snapshot.lastBackupAtMillis ?: 0L)
            .putString(KEY_LAST_BACKUP_MESSAGE, snapshot.lastBackupMessage)
            .apply()
        _snapshot.value = snapshot
    }

    fun update(transform: (AutoBackupSnapshot) -> AutoBackupSnapshot) {
        save(transform(load()))
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
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_LAST_BACKUP_MESSAGE = "last_backup_message"
    }
}
