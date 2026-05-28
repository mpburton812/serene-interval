package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.MeditationReflectionDao
import com.example.meditationparticles.data.local.MeditationReflectionEntity

class MeditationReflectionRepository(
    private val dao: MeditationReflectionDao,
) {
    suspend fun save(
        reflection: String,
        durationSeconds: Int,
        completedAt: Long,
        moodLevel: Int? = null,
    ): Long? {
        val trimmed = reflection.trim()
        if (trimmed.isBlank()) return null
        return dao.insert(
            MeditationReflectionEntity(
                reflection = trimmed,
                moodLevel = moodLevel?.coerceIn(1, 5),
                durationSeconds = durationSeconds.coerceAtLeast(0),
                completedAt = completedAt,
            ),
        )
    }

    suspend fun getById(id: Long): MeditationReflectionEntity? = dao.getById(id)
}

