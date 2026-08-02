package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.NvcEntryDao
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class NvcRepository(
    private val dao: NvcEntryDao,
    private val moodTracker: MoodTrackerRepository,
) {
    fun observeAll(): Flow<List<NvcEntryEntity>> = dao.observeAll()

    suspend fun save(entry: NvcEntryEntity): Long? {
        val hasContent = entry.observation.isNotBlank() ||
            entry.feeling.isNotBlank() ||
            entry.need.isNotBlank() ||
            entry.request.isNotBlank()
        if (!hasContent) return null
        val id = dao.insert(entry)
        MoodScale.normalize(entry.moodLevel)?.let { level ->
            moodTracker.record(
                source = MoodSource.NVC,
                level = level,
                atMillis = entry.createdAt,
                legacyTable = MoodTrackerRepository.TABLE_NVC_ENTRIES,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
