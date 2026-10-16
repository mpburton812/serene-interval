package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.MeditationReflectionDao
import com.safehaven.affirmations.data.local.MeditationReflectionEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class MeditationReflectionRepository(
    private val dao: MeditationReflectionDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)
    fun observeAll(): Flow<List<MeditationReflectionEntity>> = dao.observeAll()

    suspend fun save(
        reflection: String,
        durationSeconds: Int,
        completedAt: Long,
        moodLevel: Int? = null,
    ): Long? {
        val trimmed = reflection.trim()
        val normalizedMood = MoodScale.normalize(moodLevel)
        if (trimmed.isEmpty() && normalizedMood == null) return null
        return writer.save(
            isValid = true,
            moodLevel = normalizedMood,
            atMillis = completedAt,
            source = MoodSource.MEDITATION_REFLECTION,
            legacyTable = MoodTrackerRepository.TABLE_MEDITATION_REFLECTIONS,
            oneNoteType = OneNoteEntryType.MEDITATION_REFLECTION,
            insert = {
                dao.insert(
                    MeditationReflectionEntity(
                        reflection = trimmed,
                        moodLevel = normalizedMood,
                        durationSeconds = durationSeconds.coerceAtLeast(0),
                        completedAt = completedAt,
                    ),
                )
            },
        )
    }

    suspend fun getById(id: Long): MeditationReflectionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
