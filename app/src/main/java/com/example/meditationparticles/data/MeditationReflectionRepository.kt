package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.MeditationReflectionDao
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import kotlinx.coroutines.flow.Flow

class MeditationReflectionRepository(
    private val dao: MeditationReflectionDao,
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
        return dao.insert(
            MeditationReflectionEntity(
                reflection = trimmed,
                moodLevel = MoodScale.normalize(moodLevel),
                durationSeconds = durationSeconds.coerceAtLeast(0),
                completedAt = completedAt,
            ),
        )
    }

    suspend fun getById(id: Long): MeditationReflectionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
