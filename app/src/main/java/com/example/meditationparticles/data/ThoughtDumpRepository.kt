package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.ThoughtDumpDao
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.flow.Flow

class ThoughtDumpRepository(
    private val dao: ThoughtDumpDao,
    private val moodTracker: MoodTrackerRepository,
) {
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
        val id = dao.insert(entity)
        entity.moodLevel?.let { level ->
            val source = when (type) {
                ToolkitLogType.ANXIETY_LOG -> MoodSource.ANXIETY_LOG
                else -> MoodSource.THOUGHT_DUMP
            }
            moodTracker.record(
                source = source,
                level = level,
                atMillis = entity.createdAt,
                legacyTable = MoodTrackerRepository.TABLE_THOUGHT_DUMPS,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clear() = dao.clearAll()
}
