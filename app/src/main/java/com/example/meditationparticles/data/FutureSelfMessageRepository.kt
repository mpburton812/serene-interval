package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.FutureSelfMessageDao
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import kotlinx.coroutines.flow.Flow

class FutureSelfMessageRepository(
    private val dao: FutureSelfMessageDao,
) {
    fun observeAll(): Flow<List<FutureSelfMessageEntity>> = dao.observeAll()

    suspend fun getById(id: Long): FutureSelfMessageEntity? = dao.getById(id)

    suspend fun getPendingAfter(nowMillis: Long): List<FutureSelfMessageEntity> =
        dao.getPendingAfter(nowMillis)

    suspend fun getOverdueUndelivered(nowMillis: Long): List<FutureSelfMessageEntity> =
        dao.getOverdueUndelivered(nowMillis)

    suspend fun save(
        id: Long? = null,
        content: String,
        scheduledAtMillis: Long,
        moodLevel: Int? = null,
    ): Long? {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return null
        if (id == null || id == 0L) {
            return dao.insert(
                FutureSelfMessageEntity(
                    content = trimmed,
                    moodLevel = MoodScale.normalize(moodLevel),
                    scheduledAtMillis = scheduledAtMillis,
                ),
            )
        }
        val existing = dao.getById(id) ?: return null
        dao.update(
            existing.copy(
                content = trimmed,
                moodLevel = MoodScale.normalize(moodLevel),
                scheduledAtMillis = scheduledAtMillis,
                delivered = false,
            ),
        )
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun markDelivered(id: Long) {
        dao.markDelivered(id)
    }
}
