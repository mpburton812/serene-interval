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

    fun isConnected(): Boolean {
        val prefs = preferences.load()
        return authManager.isAvailable && !prefs.accountEmail.isNullOrBlank()
    }

    fun canAutoSync(): Boolean {
        val prefs = preferences.load()
        return isConnected() &&
            prefs.syncEnabled &&
            !prefs.sectionId.isNullOrBlank()
    }

    suspend fun enqueueSync(
        entryType: OneNoteEntryType,
        localEntryId: Long,
        manual: Boolean = false,
    ) {
        if (!isConnected()) return
        if (!manual) {
            if (!canAutoSync()) return
            if (!preferences.isEntryTypeEnabled(entryType)) return
        }
        enqueueInternal(entryType, localEntryId)
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

    suspend fun backfillExistingEntries(): Int = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext 0
        ensureSection().getOrElse { return@withContext 0 }

        var queuedCount = 0
        for (entryType in OneNoteEntryType.entries) {
            if (!preferences.isEntryTypeEnabled(entryType)) continue
            for (entryId in loadAllEntryIds(entryType)) {
                val mapping = syncDao.getMapping(entryId, entryType.name)
                if (
                    mapping?.syncStatus == OneNoteSyncStatus.SYNCED.name &&
                    !mapping.oneNotePageId.isNullOrBlank()
                ) {
                    continue
                }
                enqueueInternal(entryType, entryId)
                queuedCount++
            }
        }
        queuedCount
    }

    suspend fun deleteForEntry(entryType: OneNoteEntryType, localEntryId: Long) {
        withContext(Dispatchers.IO) {
            val mapping = syncDao.getMapping(localEntryId, entryType.name)
            syncDao.deleteQueueItemsForEntry(localEntryId, entryType.name)
            if (mapping != null) {
                syncDao.deleteMapping(localEntryId, entryType.name)
                val pageId = mapping.oneNotePageId
                if (!pageId.isNullOrBlank() && isConnected()) {
                    runCatching {
                        val token = authManager.acquireAccessToken()
                        graphClient.deletePage(token, pageId)
                    }
                }
            }
        }
    }

    suspend fun fetchNotebooks(): List<OneNoteNotebook> = withContext(Dispatchers.IO) {
        val token = authManager.acquireAccessToken()
        graphClient.listNotebooks(token)
    }

    suspend fun fetchSections(notebookId: String): List<OneNoteSection> = withContext(Dispatchers.IO) {
        val token = authManager.acquireAccessToken()
        graphClient.listSections(token, notebookId)
    }

    suspend fun applySyncTarget(
        notebookId: String,
        notebookName: String,
        sectionId: String,
        sectionName: String,
    ) {
        withContext(Dispatchers.IO) {
            preferences.setNotebook(notebookId, notebookName)
            preferences.setSection(sectionId, sectionName)
        }
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
                val existingMapping = syncDao.getMapping(item.localEntryId, item.entryType)
                val existingPageId = existingMapping?.oneNotePageId?.takeIf { it.isNotBlank() }
                val pageId = if (existingPageId != null) {
                    graphClient.updatePageContent(accessToken, existingPageId, page.html)
                    existingPageId
                } else {
                    graphClient.createPage(accessToken, sectionId, page.html)
                }
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
                            oneNotePageId = existingMappingPageId(item),
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
                            oneNotePageId = existingMappingPageId(item),
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
            val prefs = preferences.load()
            if (!prefs.sectionId.isNullOrBlank()) return@runCatching prefs.sectionId

            val token = authManager.acquireAccessToken()
            val notebook = graphClient.listNotebooks(token).firstOrNull()
                ?: throw IllegalStateException("No OneNote notebooks found for this account")
            val sections = graphClient.listSections(token, notebook.id)
            val sereneSection = sections.firstOrNull {
                it.displayName.equals(OneNoteGraphClient.SECTION_NAME, ignoreCase = true)
            }
            val section = sereneSection ?: OneNoteSection(
                id = graphClient.createSection(token, notebook.id, OneNoteGraphClient.SECTION_NAME),
                displayName = OneNoteGraphClient.SECTION_NAME,
            )
            preferences.setNotebook(notebook.id, notebook.displayName)
            preferences.setSection(section.id, section.displayName)
            section.id
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching { authManager.signOut() }
            preferences.clearConnection()
            syncDao.clearQueue()
            syncDao.clearMappings()
            WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME)
        }
    }

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) {
        syncDao.pendingQueueCount()
    }

    private suspend fun enqueueInternal(entryType: OneNoteEntryType, localEntryId: Long) {
        withContext(Dispatchers.IO) {
            syncDao.insertQueueItem(
                OneNoteSyncQueueEntity(
                    localEntryId = localEntryId,
                    entryType = entryType.name,
                    enqueuedAt = System.currentTimeMillis(),
                ),
            )
            val existing = syncDao.getMapping(localEntryId, entryType.name)
            syncDao.upsertMapping(
                OneNoteSyncMappingEntity(
                    localEntryId = localEntryId,
                    entryType = entryType.name,
                    oneNotePageId = existing?.oneNotePageId,
                    syncStatus = OneNoteSyncStatus.PENDING.name,
                ),
            )
            scheduleWorker()
        }
    }

    private suspend fun existingMappingPageId(item: OneNoteSyncQueueEntity): String? =
        syncDao.getMapping(item.localEntryId, item.entryType)?.oneNotePageId

    private suspend fun loadAllEntryIds(entryType: OneNoteEntryType): List<Long> = when (entryType) {
        OneNoteEntryType.NVC -> database.nvcEntryDao().getAll().map { it.id }
        OneNoteEntryType.REFACTORING -> database.refactoringEntryDao().getAll().map { it.id }
        OneNoteEntryType.CENTER_OF_GRAVITY -> database.centerOfGravityEntryDao().getAll().map { it.id }
        OneNoteEntryType.THOUGHT_DUMP -> database.thoughtDumpDao().getAll()
            .filter { it.logType == ToolkitLogType.THOUGHT_DUMP.name }
            .map { it.id }
        OneNoteEntryType.ANXIETY_LOG -> database.thoughtDumpDao().getAll()
            .filter { it.logType == ToolkitLogType.ANXIETY_LOG.name }
            .map { it.id }
        OneNoteEntryType.FUTURE_SELF -> database.futureSelfMessageDao().getAll().map { it.id }
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
