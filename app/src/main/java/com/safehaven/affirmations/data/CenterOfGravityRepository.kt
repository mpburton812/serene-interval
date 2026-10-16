package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.CenterOfGravityEntryDao
import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class CenterOfGravityRepository(
    private val dao: CenterOfGravityEntryDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)

    fun observeAll(): Flow<List<CenterOfGravityEntryEntity>> = dao.observeAll()

    suspend fun save(entry: CenterOfGravityEntryEntity): Long? = writer.save(
        isValid = entry.thoughtsAndFeelings.isNotBlank() || entry.bodyAndNeeds.isNotBlank(),
        moodLevel = entry.moodLevel,
        atMillis = entry.createdAt,
        source = MoodSource.CENTER_OF_GRAVITY,
        legacyTable = MoodTrackerRepository.TABLE_CENTER_OF_GRAVITY_ENTRIES,
        oneNoteType = OneNoteEntryType.CENTER_OF_GRAVITY,
        insert = { dao.insert(entry) },
    )

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
