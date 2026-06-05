package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.CenterOfGravityEntryDao
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class CenterOfGravityRepository(
    private val dao: CenterOfGravityEntryDao,
    private val moodTracker: MoodTrackerRepository,
) {
    fun observeAll(): Flow<List<CenterOfGravityEntryEntity>> = dao.observeAll()

    suspend fun save(entry: CenterOfGravityEntryEntity): Long? {
        val hasContent = entry.thoughtsAndFeelings.isNotBlank() ||
            entry.bodyAndNeeds.isNotBlank()
        if (!hasContent) return null
        val id = dao.insert(entry)
        MoodScale.normalize(entry.moodLevel)?.let { level ->
            moodTracker.record(
                source = MoodSource.CENTER_OF_GRAVITY,
                level = level,
                atMillis = entry.createdAt,
                legacyTable = MoodTrackerRepository.TABLE_CENTER_OF_GRAVITY_ENTRIES,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }
}
