package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.MeditationReflectionDao
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class MeditationReflectionRepository(
    private val dao: MeditationReflectionDao,
    private val moodTracker: MoodTrackerRepository,
) {
    fun observeAll(): Flow<List<MeditationReflectionEntity>> = dao.observeAll()

    suspend fun save(
        reflection: String,
        durationSeconds: Int,
        completedAt: Long,
        moodLevel: Int? = null,
    ): Long? {
        val trimmed = reflection.trim()
        if (trimmed.isEmpty()) return null
        val normalizedMood = MoodScale.normalize(moodLevel)
        val id = dao.insert(
            MeditationReflectionEntity(
                reflection = trimmed,
                moodLevel = normalizedMood,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                completedAt = completedAt,
            ),
        )
        normalizedMood?.let { level ->
            moodTracker.record(
                source = MoodSource.MEDITATION_REFLECTION,
                level = level,
                atMillis = completedAt,
                legacyTable = MoodTrackerRepository.TABLE_MEDITATION_REFLECTIONS,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun getById(id: Long): MeditationReflectionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
