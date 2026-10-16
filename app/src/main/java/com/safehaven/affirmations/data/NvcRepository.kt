package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.NvcEntryDao
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class NvcRepository(
    private val dao: NvcEntryDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)

    fun observeAll(): Flow<List<NvcEntryEntity>> = dao.observeAll()

    suspend fun save(entry: NvcEntryEntity): Long? = writer.save(
        isValid = entry.observation.isNotBlank() ||
            entry.feeling.isNotBlank() ||
            entry.need.isNotBlank() ||
            entry.request.isNotBlank(),
        moodLevel = entry.moodLevel,
        atMillis = entry.createdAt,
        source = MoodSource.NVC,
        legacyTable = MoodTrackerRepository.TABLE_NVC_ENTRIES,
        oneNoteType = OneNoteEntryType.NVC,
        insert = { dao.insert(entry) },
    )

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
