package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.MeditationReflectionDao
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import java.io.File
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
        audioPath: String? = null,
    ): Long? {
        val trimmed = reflection.trim()
        if (trimmed.isEmpty() && audioPath.isNullOrBlank()) return null
        return dao.insert(
            MeditationReflectionEntity(
                reflection = trimmed,
                moodLevel = moodLevel?.coerceIn(1, 5),
                audioPath = audioPath,
                durationSeconds = durationSeconds.coerceAtLeast(0),
                completedAt = completedAt,
            ),
        )
    }

    suspend fun getById(id: Long): MeditationReflectionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        val entry = dao.getById(id) ?: return
        entry.audioPath?.let { path -> File(path).delete() }
        dao.deleteById(id)
    }
}
