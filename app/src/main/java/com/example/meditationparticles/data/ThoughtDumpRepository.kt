package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.ThoughtDumpDao
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import java.io.File
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
        audioPath: String? = null,
    ): Long? {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && audioPath.isNullOrBlank()) return null
        return dao.insert(
            ThoughtDumpEntity(
                content = trimmed,
                logType = type.name,
                moodLevel = moodLevel?.coerceIn(1, 5),
                audioPath = audioPath,
            ),
        )
    }

    suspend fun deleteEntry(id: Long) {
        val entry = dao.getById(id) ?: return
        entry.audioPath?.let { path -> File(path).delete() }
        dao.deleteById(id)
    }

    suspend fun clear() = dao.clearAll()
}
