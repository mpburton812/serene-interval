package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.RefactoringEntryDao
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import java.io.File
import kotlinx.coroutines.flow.Flow

class RefactoringRepository(
    private val dao: RefactoringEntryDao,
) {
    fun observeAll(): Flow<List<RefactoringEntryEntity>> = dao.observeAll()

    suspend fun save(entry: RefactoringEntryEntity): Long? {
        val hasContent = entry.interpretation.isNotBlank() ||
            entry.actualFacts.isNotBlank() ||
            entry.explanation1.isNotBlank() ||
            entry.explanation2.isNotBlank() ||
            entry.explanation3.isNotBlank() ||
            !entry.interpretationAudioPath.isNullOrBlank() ||
            !entry.actualFactsAudioPath.isNullOrBlank() ||
            !entry.explanation1AudioPath.isNullOrBlank() ||
            !entry.explanation2AudioPath.isNullOrBlank() ||
            !entry.explanation3AudioPath.isNullOrBlank()
        if (!hasContent) return null
        return dao.insert(entry)
    }

    suspend fun deleteEntry(id: Long) {
        val entry = dao.getById(id) ?: return
        listOf(
            entry.interpretationAudioPath,
            entry.actualFactsAudioPath,
            entry.explanation1AudioPath,
            entry.explanation2AudioPath,
            entry.explanation3AudioPath,
        ).forEach { path ->
            path?.let { File(it).delete() }
        }
        dao.deleteById(id)
    }
}
