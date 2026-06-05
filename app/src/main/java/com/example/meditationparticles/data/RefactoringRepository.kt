package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.RefactoringEntryDao
import com.example.meditationparticles.data.local.RefactoringEntryEntity
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
            entry.explanation3.isNotBlank()
        if (!hasContent) return null
        return dao.insert(entry)
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
