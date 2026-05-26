package com.example.meditationparticles.data.onenote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OneNotePrefsSnapshot(
    val syncEnabled: Boolean = false,
    val accountEmail: String? = null,
    val notebookId: String? = null,
    val notebookName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val enabledEntryTypes: Set<OneNoteEntryType> = OneNoteEntryType.entries.toSet(),
    val lastSyncAtMillis: Long? = null,
    val lastError: String? = null,
)

class OneNotePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<OneNotePrefsSnapshot> = _snapshot.asStateFlow()

    fun load(): OneNotePrefsSnapshot = OneNotePrefsSnapshot(
        syncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, false),
        accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, null)?.takeIf { it.isNotBlank() },
        notebookId = prefs.getString(KEY_NOTEBOOK_ID, null)?.takeIf { it.isNotBlank() },
        notebookName = prefs.getString(KEY_NOTEBOOK_NAME, null)?.takeIf { it.isNotBlank() },
        sectionId = prefs.getString(KEY_SECTION_ID, null)?.takeIf { it.isNotBlank() },
        sectionName = prefs.getString(KEY_SECTION_NAME, null)?.takeIf { it.isNotBlank() },
        enabledEntryTypes = loadEnabledEntryTypes(),
        lastSyncAtMillis = prefs.getLong(KEY_LAST_SYNC_AT, 0L).takeIf { it > 0L },
        lastError = prefs.getString(KEY_LAST_ERROR, null)?.takeIf { it.isNotBlank() },
    )

    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
        publish()
    }

    fun setAccountEmail(email: String?) {
        prefs.edit().putString(KEY_ACCOUNT_EMAIL, email?.trim().orEmpty()).apply()
        publish()
    }

    fun setNotebook(notebookId: String?, notebookName: String?) {
        prefs.edit()
            .putString(KEY_NOTEBOOK_ID, notebookId?.trim().orEmpty())
            .putString(KEY_NOTEBOOK_NAME, notebookName?.trim().orEmpty())
            .apply()
        publish()
    }

    fun setSection(sectionId: String?, sectionName: String?) {
        prefs.edit()
            .putString(KEY_SECTION_ID, sectionId?.trim().orEmpty())
            .putString(KEY_SECTION_NAME, sectionName?.trim().orEmpty())
            .apply()
        publish()
    }

    fun setSectionId(sectionId: String?) {
        setSection(sectionId, prefs.getString(KEY_SECTION_NAME, null))
    }

    fun isEntryTypeEnabled(entryType: OneNoteEntryType): Boolean =
        entryType in loadEnabledEntryTypes()

    fun setEntryTypeEnabled(entryType: OneNoteEntryType, enabled: Boolean) {
        val current = loadEnabledEntryTypes().toMutableSet()
        if (enabled) {
            current.add(entryType)
        } else {
            current.remove(entryType)
        }
        prefs.edit()
            .putString(KEY_ENABLED_ENTRY_TYPES, current.joinToString(",") { it.name })
            .apply()
        publish()
    }

    fun setLastSyncAt(millis: Long?) {
        prefs.edit().putLong(KEY_LAST_SYNC_AT, millis ?: 0L).apply()
        publish()
    }

    fun setLastError(message: String?) {
        prefs.edit().putString(KEY_LAST_ERROR, message?.trim().orEmpty()).apply()
        publish()
    }

    fun clearConnection() {
        prefs.edit()
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_NOTEBOOK_ID)
            .remove(KEY_NOTEBOOK_NAME)
            .remove(KEY_SECTION_ID)
            .remove(KEY_SECTION_NAME)
            .remove(KEY_ENABLED_ENTRY_TYPES)
            .putBoolean(KEY_SYNC_ENABLED, false)
            .remove(KEY_LAST_SYNC_AT)
            .remove(KEY_LAST_ERROR)
            .apply()
        publish()
    }

    private fun loadEnabledEntryTypes(): Set<OneNoteEntryType> {
        val stored = prefs.getString(KEY_ENABLED_ENTRY_TYPES, null)?.takeIf { it.isNotBlank() }
            ?: return OneNoteEntryType.entries.toSet()
        val parsed = stored.split(',')
            .mapNotNull { name -> runCatching { OneNoteEntryType.valueOf(name.trim()) }.getOrNull() }
            .toSet()
        return parsed.ifEmpty { OneNoteEntryType.entries.toSet() }
    }

    private fun publish() {
        _snapshot.value = load()
    }

    companion object {
        private const val PREFS_NAME = "one_note_preferences"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_NOTEBOOK_ID = "notebook_id"
        private const val KEY_NOTEBOOK_NAME = "notebook_name"
        private const val KEY_SECTION_ID = "section_id"
        private const val KEY_SECTION_NAME = "section_name"
        private const val KEY_ENABLED_ENTRY_TYPES = "enabled_entry_types"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_LAST_ERROR = "last_error"
    }
}
