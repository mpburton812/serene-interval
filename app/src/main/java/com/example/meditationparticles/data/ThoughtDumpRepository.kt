package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.ThoughtDumpDao
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.flow.Flow

class ThoughtDumpRepository(
    private val dao: ThoughtDumpDao,
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
        return dao.insert(
            ThoughtDumpEntity(
                content = trimmed,
                logType = type.name,
                moodLevel = MoodScale.normalize(moodLevel),
            ),
        )
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clear() = dao.clearAll()
}
