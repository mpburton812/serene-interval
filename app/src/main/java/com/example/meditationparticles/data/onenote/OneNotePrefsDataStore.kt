package com.example.meditationparticles.data.onenote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.datastore.core.DataStoreFactory
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream

internal class OneNotePrefsEncryptor(context: Context) {
    private val appContext = context.applicationContext

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREF_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://$KEYSET_MASTER_KEY_ALIAS")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: ByteArray): ByteArray = aead.encrypt(plaintext, ASSOCIATED_DATA)

    fun decrypt(ciphertext: ByteArray): ByteArray = aead.decrypt(ciphertext, ASSOCIATED_DATA)

    companion object {
        private const val KEYSET_NAME = "one_note_prefs_keyset"
        private const val KEYSET_PREF_NAME = "one_note_prefs_keyset_pref"
        private const val KEYSET_MASTER_KEY_ALIAS = "one_note_prefs_master_key"
        private val ASSOCIATED_DATA = "one_note_prefs".toByteArray(Charsets.UTF_8)
    }
}

internal class OneNotePrefsSerializer(
    private val encryptor: OneNotePrefsEncryptor,
) : Serializer<OneNotePrefsSnapshot> {
    override val defaultValue: OneNotePrefsSnapshot = OneNotePrefsSnapshot()

    override suspend fun readFrom(input: InputStream): OneNotePrefsSnapshot {
        val encrypted = input.readBytes()
        if (encrypted.isEmpty()) return defaultValue
        return runCatching {
            val plaintext = encryptor.decrypt(encrypted)
            decodeSnapshot(String(plaintext, Charsets.UTF_8))
        }.getOrDefault(defaultValue)
    }

    override suspend fun writeTo(t: OneNotePrefsSnapshot, output: OutputStream) {
        val plaintext = encodeSnapshot(t).toByteArray(Charsets.UTF_8)
        output.write(encryptor.encrypt(plaintext))
    }
}

internal class OneNotePrefsDataStore(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val encryptor = OneNotePrefsEncryptor(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val dataStore: DataStore<OneNotePrefsSnapshot> = DataStoreFactory.create(
        serializer = OneNotePrefsSerializer(encryptor),
        scope = scope,
        produceFile = { appContext.dataStoreFile(DATASTORE_FILE_NAME) },
    )

    suspend fun ensureMigrated() {
        val dataFile = appContext.dataStoreFile(DATASTORE_FILE_NAME)
        if (dataFile.exists() && dataFile.length() > 0L) return
        val legacy = readLegacyEncryptedPrefs() ?: return
        dataStore.updateData { legacy }
        appContext.deleteSharedPreferences(LEGACY_PREFS_NAME)
        File(appContext.applicationInfo.dataDir, "shared_prefs/$LEGACY_PREFS_NAME.xml")
            .takeIf { it.exists() }
            ?.delete()
    }

    fun ensureMigratedBlocking() {
        runBlocking(Dispatchers.IO) { ensureMigrated() }
    }

    suspend fun read(): OneNotePrefsSnapshot = dataStore.data.first()

    suspend fun update(transform: (OneNotePrefsSnapshot) -> OneNotePrefsSnapshot) {
        dataStore.updateData(transform)
    }

    private fun readLegacyEncryptedPrefs(): OneNotePrefsSnapshot? {
        val legacyFile = File(appContext.applicationInfo.dataDir, "shared_prefs/$LEGACY_PREFS_NAME.xml")
        if (!legacyFile.exists()) return null
        return runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                appContext,
                LEGACY_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            OneNotePrefsSnapshot(
                syncEnabled = prefs.getBoolean(KEY_SYNC_ENABLED, false),
                accountEmail = prefs.getString(KEY_ACCOUNT_EMAIL, null)?.takeIf { it.isNotBlank() },
                notebookId = prefs.getString(KEY_NOTEBOOK_ID, null)?.takeIf { it.isNotBlank() },
                notebookName = prefs.getString(KEY_NOTEBOOK_NAME, null)?.takeIf { it.isNotBlank() },
                sectionId = prefs.getString(KEY_SECTION_ID, null)?.takeIf { it.isNotBlank() },
                sectionName = prefs.getString(KEY_SECTION_NAME, null)?.takeIf { it.isNotBlank() },
                enabledEntryTypes = loadEnabledEntryTypes(prefs.getString(KEY_ENABLED_ENTRY_TYPES, null)),
                lastSyncAtMillis = prefs.getLong(KEY_LAST_SYNC_AT, 0L).takeIf { it > 0L },
                lastError = prefs.getString(KEY_LAST_ERROR, null)?.takeIf { it.isNotBlank() },
                moodScaleBackfillDone = prefs.getBoolean(KEY_MOOD_SCALE_BACKFILL_DONE, false),
            )
        }.getOrNull()
    }

    companion object {
        private const val DATASTORE_FILE_NAME = "one_note_prefs.pb"
        private const val LEGACY_PREFS_NAME = "one_note_preferences"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_ACCOUNT_EMAIL = "account_email"
        private const val KEY_NOTEBOOK_ID = "notebook_id"
        private const val KEY_NOTEBOOK_NAME = "notebook_name"
        private const val KEY_SECTION_ID = "section_id"
        private const val KEY_SECTION_NAME = "section_name"
        private const val KEY_ENABLED_ENTRY_TYPES = "enabled_entry_types"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_MOOD_SCALE_BACKFILL_DONE = "mood_scale_backfill_done"
    }
}

private fun loadEnabledEntryTypes(stored: String?): Set<OneNoteEntryType> {
    val raw = stored?.takeIf { it.isNotBlank() } ?: return OneNoteEntryType.entries.toSet()
    val parsed = raw.split(',')
        .mapNotNull { name -> runCatching { OneNoteEntryType.valueOf(name.trim()) }.getOrNull() }
        .toSet()
    return parsed.ifEmpty { OneNoteEntryType.entries.toSet() }
}

private fun encodeSnapshot(snapshot: OneNotePrefsSnapshot): String = JSONObject().apply {
    put("sync_enabled", snapshot.syncEnabled)
    put("account_email", snapshot.accountEmail.orEmpty())
    put("notebook_id", snapshot.notebookId.orEmpty())
    put("notebook_name", snapshot.notebookName.orEmpty())
    put("section_id", snapshot.sectionId.orEmpty())
    put("section_name", snapshot.sectionName.orEmpty())
    put(
        "enabled_entry_types",
        snapshot.enabledEntryTypes.joinToString(",") { it.name },
    )
    put("last_sync_at", snapshot.lastSyncAtMillis ?: 0L)
    put("last_error", snapshot.lastError.orEmpty())
    put("mood_scale_backfill_done", snapshot.moodScaleBackfillDone)
}.toString()

private fun decodeSnapshot(json: String): OneNotePrefsSnapshot {
    val obj = JSONObject(json)
    return OneNotePrefsSnapshot(
        syncEnabled = obj.optBoolean("sync_enabled", false),
        accountEmail = obj.optString("account_email").takeIf { it.isNotBlank() },
        notebookId = obj.optString("notebook_id").takeIf { it.isNotBlank() },
        notebookName = obj.optString("notebook_name").takeIf { it.isNotBlank() },
        sectionId = obj.optString("section_id").takeIf { it.isNotBlank() },
        sectionName = obj.optString("section_name").takeIf { it.isNotBlank() },
        enabledEntryTypes = loadEnabledEntryTypes(obj.optString("enabled_entry_types")),
        lastSyncAtMillis = obj.optLong("last_sync_at", 0L).takeIf { it > 0L },
        lastError = obj.optString("last_error").takeIf { it.isNotBlank() },
        moodScaleBackfillDone = obj.optBoolean("mood_scale_backfill_done", false),
    )
}
