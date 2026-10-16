package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.RefactoringEntryDao
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class RefactoringRepository(
    private val dao: RefactoringEntryDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)

    fun observeAll(): Flow<List<RefactoringEntryEntity>> = dao.observeAll()

    suspend fun save(entry: RefactoringEntryEntity): Long? = writer.save(
        isValid = entry.interpretation.isNotBlank() ||
            entry.actualFacts.isNotBlank() ||
            entry.explanation1.isNotBlank() ||
            entry.explanation2.isNotBlank() ||
            entry.explanation3.isNotBlank(),
        moodLevel = entry.moodLevel,
        atMillis = entry.createdAt,
        source = MoodSource.REFACTORING,
        legacyTable = MoodTrackerRepository.TABLE_REFACTORING_ENTRIES,
        oneNoteType = OneNoteEntryType.REFACTORING,
        insert = { dao.insert(entry) },
    )

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
