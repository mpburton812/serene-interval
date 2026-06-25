package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.AffirmationReviewSessionDao
import com.example.meditationparticles.data.local.AffirmationReviewSessionEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow

class AffirmationReviewSessionRepository(
    private val dao: AffirmationReviewSessionDao,
    private val moodTracker: MoodTrackerRepository,
) {
    suspend fun save(
        notes: String,
        affirmationCount: Int,
        completedAt: Long,
        moodLevel: Int? = null,
    ): Long {
        val normalizedMood = MoodScale.normalize(moodLevel)
        val id = dao.insert(
            AffirmationReviewSessionEntity(
                notes = notes.trim(),
                moodLevel = normalizedMood,
                affirmationCount = affirmationCount.coerceAtLeast(0),
                completedAt = completedAt,
            ),
        )
        normalizedMood?.let { level ->
            moodTracker.record(
                source = MoodSource.AFFIRMATION_REVIEW,
                level = level,
                atMillis = completedAt,
                legacyTable = MoodTrackerRepository.TABLE_AFFIRMATION_REVIEW_SESSIONS,
                legacyRowId = id,
            )
        }
        return id
    }

    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<AffirmationReviewSessionEntity>> =
        dao.observeInRange(startMillis, endMillis)

    suspend fun getById(id: Long): AffirmationReviewSessionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
