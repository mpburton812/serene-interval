package com.example.meditationparticles.data.onenote

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.meditationparticles.data.local.OneNoteSyncDao
import com.example.meditationparticles.data.local.OneNoteSyncMappingEntity
import com.example.meditationparticles.data.local.OneNoteSyncQueueEntity
import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.domain.onenote.OneNoteSyncStatus
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OneNoteSyncRepository(
    private val context: Context,
    private val preferences: OneNotePreferences,
    private val authManager: OneNoteAuthManager,
    private val graphClient: OneNoteGraphClient,
    private val syncDao: OneNoteSyncDao,
    private val database: SereneDatabase,
) {
    private val appContext = context.applicationContext

    fun canAutoSync(): Boolean {
        val prefs = preferences.load()
        return authManager.isAvailable &&
            prefs.syncEnabled &&
            !prefs.accountEmail.isNullOrBlank() &&
            !prefs.sectionId.isNullOrBlank()
    }

    suspend fun enqueueSync(entryType: OneNoteEntryType, localEntryId: Long) {
        if (!canAutoSync()) return
        withContext(Dispatchers.IO) {
            syncDao.insertQueueItem(
                OneNoteSyncQueueEntity(
                    localEntryId = localEntryId,
                    entryType = entryType.name,
                    enqueuedAt = System.currentTimeMillis(),
                ),
            )
            syncDao.upsertMapping(
                OneNoteSyncMappingEntity(
                    localEntryId = localEntryId,
                    entryType = entryType.name,
                    syncStatus = OneNoteSyncStatus.PENDING.name,
                ),
            )
            scheduleWorker()
        }
    }

    suspend fun syncNow(): OneNoteSyncRunResult = withContext(Dispatchers.IO) {
        if (!authManager.isAvailable) {
            return@withContext OneNoteSyncRunResult(
                syncedCount = 0,
                failedCount = 0,
                message = "OneNote sync is not configured.",
            )
        }
        val prefs = preferences.load()
        if (prefs.accountEmail.isNullOrBlank()) {
            return@withContext OneNoteSyncRunResult(
                syncedCount = 0,
                failedCount = 0,
                message = "Connect a Microsoft account first.",
            )
        }
        processQueue(force = true)
    }

    suspend fun processQueue(force: Boolean = false): OneNoteSyncRunResult = withContext(Dispatchers.IO) {
        val prefs = preferences.load()
        if (!force && !prefs.syncEnabled) {
            return@withContext OneNoteSyncRunResult(message = "Auto-sync is disabled.")
        }
        if (prefs.accountEmail.isNullOrBlank()) {
            return@withContext OneNoteSyncRunResult(message = "Not connected to OneNote.")
        }

        val sectionId = prefs.sectionId ?: run {
            val ensured = ensureSection()
            if (ensured.isFailure) {
                val message = ensured.exceptionOrNull()?.message ?: "Could not prepare OneNote section."
                preferences.setLastError(message)
                return@withContext OneNoteSyncRunResult(message = message)
            }
            ensured.getOrThrow()
        }

        val queue = OneNoteSyncQueueLogic.eligibleForProcessing(syncDao.getAllQueueItems())
        if (queue.isEmpty()) {
            return@withContext OneNoteSyncRunResult(message = "Nothing pending to sync.")
        }

        var syncedCount = 0
        var failedCount = 0
        var lastError: String? = null

        for (item in queue) {
            val entryType = runCatching { OneNoteEntryType.valueOf(item.entryType) }.getOrNull()
            if (entryType == null) {
                syncDao.deleteQueueItem(item.id)
                continue
            }
            val payload = loadEntry(entryType, item.localEntryId)
            if (payload == null) {
                syncDao.deleteQueueItem(item.id)
                continue
            }

            try {
                val accessToken = authManager.acquireAccessToken()
                val page = OneNotePageRenderer.render(entryType, payload)
                val pageId = graphClient.createPage(accessToken, sectionId, page.html)
                syncDao.upsertMapping(
                    OneNoteSyncMappingEntity(
                        localEntryId = item.localEntryId,
                        entryType = item.entryType,
                        oneNotePageId = pageId,
                        syncStatus = OneNoteSyncStatus.SYNCED.name,
                        syncedAt = System.currentTimeMillis(),
                    ),
                )
                syncDao.deleteQueueItem(item.id)
                syncedCount++
            } catch (error: OneNoteAuthRequiredException) {
                lastError = error.message
                preferences.setLastError(lastError)
                break
            } catch (error: Exception) {
                lastError = error.message ?: "Sync failed"
                failedCount++
                val nextRetry = item.retryCount + 1
                if (OneNoteSyncQueueLogic.shouldDropItem(item.copy(retryCount = nextRetry))) {
                    syncDao.upsertMapping(
                        OneNoteSyncMappingEntity(
                            localEntryId = item.localEntryId,
                            entryType = item.entryType,
                            syncStatus = OneNoteSyncStatus.FAILED.name,
                            lastError = lastError,
                        ),
                    )
                    syncDao.deleteQueueItem(item.id)
                } else {
                    syncDao.updateQueueRetryCount(item.id, nextRetry)
                    syncDao.upsertMapping(
                        OneNoteSyncMappingEntity(
                            localEntryId = item.localEntryId,
                            entryType = item.entryType,
                            syncStatus = OneNoteSyncStatus.PENDING.name,
                            lastError = lastError,
                        ),
                    )
                }
            }
        }

        if (syncedCount > 0) {
            preferences.setLastSyncAt(System.currentTimeMillis())
            preferences.setLastError(null)
        } else if (lastError != null) {
            preferences.setLastError(lastError)
        }

        OneNoteSyncRunResult(
            syncedCount = syncedCount,
            failedCount = failedCount,
            message = when {
                syncedCount > 0 && failedCount == 0 -> "Synced $syncedCount ${if (syncedCount == 1) "entry" else "entries"}."
                syncedCount > 0 -> "Synced $syncedCount; $failedCount failed."
                failedCount > 0 -> lastError ?: "Sync failed."
                else -> lastError ?: "Nothing synced."
            },
        )
    }

    suspend fun ensureSection(): Result<String> = withContext(Dispatchers.IO) {
        if (!authManager.isAvailable) {
            return@withContext Result.failure(IllegalStateException("OneNote sync is not configured"))
        }
        runCatching {
            val existing = preferences.load().sectionId
            if (!existing.isNullOrBlank()) return@runCatching existing
            val token = authManager.acquireAccessToken()
            val sectionId = graphClient.ensureSereneIntervalSection(token)
            preferences.setSectionId(sectionId)
            sectionId
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { authManager.signOut() }
            preferences.clearConnection()
            syncDao.clearQueue()
            syncDao.clearMappings()
        }
    }

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) {
        syncDao.pendingQueueCount()
    }

    private suspend fun loadEntry(entryType: OneNoteEntryType, localEntryId: Long): Any? =
        when (entryType) {
            OneNoteEntryType.NVC -> database.nvcEntryDao().getById(localEntryId)
            OneNoteEntryType.REFACTORING -> database.refactoringEntryDao().getById(localEntryId)
            OneNoteEntryType.CENTER_OF_GRAVITY -> database.centerOfGravityEntryDao().getById(localEntryId)
            OneNoteEntryType.THOUGHT_DUMP -> database.thoughtDumpDao().getById(localEntryId)?.takeIf {
                it.logType == ToolkitLogType.THOUGHT_DUMP.name
            }
            OneNoteEntryType.ANXIETY_LOG -> database.thoughtDumpDao().getById(localEntryId)?.takeIf {
                it.logType == ToolkitLogType.ANXIETY_LOG.name
            }
            OneNoteEntryType.FUTURE_SELF -> database.futureSelfMessageDao().getById(localEntryId)
        }

    private fun scheduleWorker() {
        val request = OneTimeWorkRequestBuilder<OneNoteSyncWorker>().build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }

    companion object {
        const val WORK_NAME = "one_note_sync"
    }
}

data class OneNoteSyncRunResult(
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val message: String? = null,
)
