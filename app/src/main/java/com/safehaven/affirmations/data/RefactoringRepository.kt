package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.RefactoringEntryDao
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class RefactoringRepository(
    private val dao: RefactoringEntryDao,
    private val moodTracker: MoodTrackerRepository,
) {
    fun observeAll(): Flow<List<RefactoringEntryEntity>> = dao.observeAll()

    suspend fun save(entry: RefactoringEntryEntity): Long? {
        val hasContent = entry.interpretation.isNotBlank() ||
            entry.actualFacts.isNotBlank() ||
            entry.explanation1.isNotBlank() ||
            entry.explanation2.isNotBlank() ||
            entry.explanation3.isNotBlank()
        if (!hasContent) return null
        val id = dao.insert(entry)
        MoodScale.normalize(entry.moodLevel)?.let { level ->
            moodTracker.record(
                source = MoodSource.REFACTORING,
                level = level,
                atMillis = entry.createdAt,
                legacyTable = MoodTrackerRepository.TABLE_REFACTORING_ENTRIES,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
