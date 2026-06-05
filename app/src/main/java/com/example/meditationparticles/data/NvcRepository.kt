package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.NvcEntryDao
import com.example.meditationparticles.data.local.NvcEntryEntity
import kotlinx.coroutines.flow.Flow

class NvcRepository(
    private val dao: NvcEntryDao,
) {
    fun observeAll(): Flow<List<NvcEntryEntity>> = dao.observeAll()

    suspend fun save(entry: NvcEntryEntity): Long? {
        val hasContent = entry.observation.isNotBlank() ||
            entry.feeling.isNotBlank() ||
            entry.need.isNotBlank() ||
            entry.request.isNotBlank()
        if (!hasContent) return null
        return dao.insert(entry)
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
