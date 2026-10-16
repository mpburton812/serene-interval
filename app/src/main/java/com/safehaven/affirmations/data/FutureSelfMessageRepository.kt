package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.FutureSelfMessageDao
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class FutureSelfMessageRepository(
    private val dao: FutureSelfMessageDao,
    moodTracker: MoodTrackerRepository,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)
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
            writer.recordMood(
                moodLevel = normalizedMood,
                atMillis = entity.createdAtMillis,
                source = MoodSource.FUTURE_SELF,
                legacyTable = MoodTrackerRepository.TABLE_FUTURE_SELF_MESSAGES,
                legacyRowId = newId,
            )
            writer.enqueue(OneNoteEntryType.FUTURE_SELF, newId)
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
        writer.recordMood(
            moodLevel = normalizedMood,
            atMillis = existing.createdAtMillis,
            source = MoodSource.FUTURE_SELF,
            legacyTable = MoodTrackerRepository.TABLE_FUTURE_SELF_MESSAGES,
            legacyRowId = id,
        )
        writer.enqueue(OneNoteEntryType.FUTURE_SELF, id)
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    suspend fun markDelivered(id: Long) {
        dao.markDelivered(id)
    }
}
