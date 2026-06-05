package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.CenterOfGravityEntryDao
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import kotlinx.coroutines.flow.Flow

class CenterOfGravityRepository(
    private val dao: CenterOfGravityEntryDao,
) {
    fun observeAll(): Flow<List<CenterOfGravityEntryEntity>> = dao.observeAll()

    suspend fun save(entry: CenterOfGravityEntryEntity): Long? {
        val hasContent = entry.thoughtsAndFeelings.isNotBlank() ||
            entry.bodyAndNeeds.isNotBlank()
        if (!hasContent) return null
        return dao.insert(entry)
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
