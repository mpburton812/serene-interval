package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.ThoughtDumpDao
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import com.safehaven.affirmations.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.flow.Flow

class ThoughtDumpRepository(
    private val dao: ThoughtDumpDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)
    fun observeEntries(type: ToolkitLogType): Flow<List<ThoughtDumpEntity>> =
        dao.observeByType(type.name)

    val latestDump: Flow<ThoughtDumpEntity?> = dao.observeLatest()

    suspend fun save(
        type: ToolkitLogType,
        content: String,
        moodLevel: Int? = null,
    ): Long? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return null
        val entity = ThoughtDumpEntity(
            content = trimmed,
            logType = type.name,
            moodLevel = MoodScale.normalize(moodLevel),
        )
        return writer.save(
            isValid = true,
            moodLevel = entity.moodLevel,
            atMillis = entity.createdAt,
            source = when (type) {
                ToolkitLogType.ANXIETY_LOG -> MoodSource.ANXIETY_LOG
                else -> MoodSource.THOUGHT_DUMP
            },
            legacyTable = MoodTrackerRepository.TABLE_THOUGHT_DUMPS,
            oneNoteType = when (type) {
                ToolkitLogType.ANXIETY_LOG -> OneNoteEntryType.ANXIETY_LOG
                else -> OneNoteEntryType.THOUGHT_DUMP
            },
            insert = { dao.insert(entity) },
        )
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clear() = dao.clearAll()
}
