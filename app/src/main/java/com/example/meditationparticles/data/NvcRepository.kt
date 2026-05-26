package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.NvcEntryDao
import com.example.meditationparticles.data.local.NvcEntryEntity
import java.io.File
import kotlinx.coroutines.flow.Flow

class NvcRepository(
    private val dao: NvcEntryDao,
) {
    fun observeAll(): Flow<List<NvcEntryEntity>> = dao.observeAll()

    suspend fun save(entry: NvcEntryEntity): Long? {
        val hasContent = entry.observation.isNotBlank() ||
            entry.feeling.isNotBlank() ||
            entry.need.isNotBlank() ||
            entry.request.isNotBlank() ||
            !entry.observationAudioPath.isNullOrBlank() ||
            !entry.feelingAudioPath.isNullOrBlank() ||
            !entry.needAudioPath.isNullOrBlank() ||
            !entry.requestAudioPath.isNullOrBlank()
        if (!hasContent) return null
        return dao.insert(entry)
    }

    suspend fun deleteEntry(id: Long) {
        val entry = dao.getById(id) ?: return
        listOf(
            entry.observationAudioPath,
            entry.feelingAudioPath,
            entry.needAudioPath,
            entry.requestAudioPath,
        ).forEach { path ->
            path?.let { File(it).delete() }
        }
        dao.deleteById(id)
    }
}
