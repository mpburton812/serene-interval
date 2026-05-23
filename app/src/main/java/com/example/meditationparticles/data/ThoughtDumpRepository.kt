package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.ThoughtDumpDao
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import kotlinx.coroutines.flow.Flow

class ThoughtDumpRepository(
    private val dao: ThoughtDumpDao,
) {
    val latestDump: Flow<ThoughtDumpEntity?> = dao.observeLatest()

    suspend fun save(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        dao.clearAll()
        dao.insert(ThoughtDumpEntity(content = trimmed))
    }

    suspend fun clear() = dao.clearAll()
}
