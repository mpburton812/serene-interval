package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.FutureSelfMessageDao
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class FutureSelfMessageRepository(
    private val dao: FutureSelfMessageDao,
    private val moodTracker: MoodTrackerRepository,
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
        val normalizedMood = MoodScale.normalize(moodLevel)
        if (id == null || id == 0L) {
            val entity = FutureSelfMessageEntity(
                content = trimmed,
                moodLevel = normalizedMood,
                scheduledAtMillis = scheduledAtMillis,
            )
            val newId = dao.insert(entity)
            normalizedMood?.let { level ->
                moodTracker.record(
                    source = MoodSource.FUTURE_SELF,
                    level = level,
                    atMillis = entity.createdAtMillis,
                    legacyTable = MoodTrackerRepository.TABLE_FUTURE_SELF_MESSAGES,
                    legacyRowId = newId,
                )
            }
            return newId
        }
        val existing = dao.getById(id) ?: return null
        dao.update(
            existing.copy(
                content = trimmed,
                moodLevel = normalizedMood,
                scheduledAtMillis = scheduledAtMillis,
                delivered = false,
            ),
        )
        normalizedMood?.let { level ->
            moodTracker.record(
                source = MoodSource.FUTURE_SELF,
                level = level,
                atMillis = existing.createdAtMillis,
                legacyTable = MoodTrackerRepository.TABLE_FUTURE_SELF_MESSAGES,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun markDelivered(id: Long) {
        dao.markDelivered(id)
    }
}
