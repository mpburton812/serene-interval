package com.example.meditationparticles.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.meditationparticles.data.export.AppDataExporter
import com.example.meditationparticles.data.local.SereneDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AutoBackupResult(
    val success: Boolean,
    val message: String,
    val fileName: String? = null,
)

class AutoBackupManager(
    private val context: Context,
    private val preferences: AutoBackupPreferences,
    private val exporter: AppDataExporter = AppDataExporter(context),
) {
    suspend fun runBackup(force: Boolean = false): AutoBackupResult = withContext(Dispatchers.IO) {
        val snapshot = preferences.load()
        if (!force && !snapshot.autoBackupEnabled) {
            return@withContext AutoBackupResult(false, "Automatic backup is off.")
        }
        val treeUri = snapshot.destinationTreeUri?.let(Uri::parse)
            ?: return@withContext AutoBackupResult(false, "Choose a backup folder first.")

        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext AutoBackupResult(false, "Could not access the backup folder.")

        if (!root.canWrite()) {
            return@withContext AutoBackupResult(false, "Sway cannot write to the selected folder.")
        }

        runCatching {
            val json = exporter.buildExportJson()
            val fileName = buildBackupFileName()
            val existing = root.findFile(fileName)
            existing?.delete()
            val created = root.createFile("application/json", fileName)
                ?: error("Could not create backup file.")
            context.contentResolver.openOutputStream(created.uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open backup file for writing.")
            pruneOldBackups(root)
            val message = "Saved $fileName"
            preferences.recordBackupResult(System.currentTimeMillis(), message)
            AutoBackupResult(success = true, message = message, fileName = fileName)
        }.getOrElse { error ->
            val message = error.message ?: "Backup failed."
            preferences.recordBackupResult(System.currentTimeMillis(), message)
            AutoBackupResult(success = false, message = message)
        }
    }

    suspend fun hasProtectableUserData(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val sqlite = SereneDatabase.getInstance(context).openHelper.readableDatabase
            sqlite.query(USER_DATA_EXISTS_SQL).use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) == 1
            }
        }.getOrDefault(false)
    }

    fun shouldShowBackupPrompt(snapshot: AutoBackupSnapshot, hasUserData: Boolean): Boolean =
        hasUserData &&
            !snapshot.autoBackupEnabled &&
            !snapshot.backupPromptDismissed &&
            !snapshot.backupPromptShown

    companion object {
        private const val MAX_RETAINED_BACKUPS = 14
        private val FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm", Locale.US)

        private const val USER_DATA_EXISTS_SQL = """
            SELECT
                EXISTS(SELECT 1 FROM mood_entries LIMIT 1)
                OR EXISTS(SELECT 1 FROM thought_dumps LIMIT 1)
                OR EXISTS(SELECT 1 FROM affirmations LIMIT 1)
                OR EXISTS(SELECT 1 FROM future_self_messages LIMIT 1)
                OR EXISTS(SELECT 1 FROM refactoring_entries LIMIT 1)
                OR EXISTS(SELECT 1 FROM center_of_gravity_entries LIMIT 1)
                OR EXISTS(SELECT 1 FROM nvc_entries LIMIT 1)
                OR EXISTS(SELECT 1 FROM meditation_reflections LIMIT 1)
                OR EXISTS(SELECT 1 FROM hearts_entries LIMIT 1)
                OR EXISTS(SELECT 1 FROM living_tree_people LIMIT 1)
        """

        fun buildBackupFileName(now: Instant = Instant.now()): String {
            val stamp = FILE_NAME_FORMATTER.format(now.atZone(ZoneId.systemDefault()))
            return "sway-backup-$stamp.json"
        }

        private fun pruneOldBackups(root: DocumentFile) {
            val backups = root.listFiles()
                .filter { file ->
                    file.isFile && file.name?.startsWith("sway-backup-") == true &&
                        file.name?.endsWith(".json") == true
                }
                .sortedByDescending { it.lastModified() }
            backups.drop(MAX_RETAINED_BACKUPS).forEach { it.delete() }
        }
    }
}
