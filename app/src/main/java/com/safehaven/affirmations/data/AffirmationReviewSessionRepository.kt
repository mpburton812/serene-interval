package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.AffirmationReviewSessionDao
import com.safehaven.affirmations.data.local.AffirmationReviewSessionEntity
import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import kotlinx.coroutines.flow.Flow

class AffirmationReviewSessionRepository(
    private val dao: AffirmationReviewSessionDao,
    moodTracker: MoodTrackerRepository,
    private val listKind: AffirmationListKind = AffirmationListKind.Affirmations,
    oneNoteSync: OneNoteSyncRepository? = null,
) {
    private val kindKey = listKind.name
    private val writer = JournalEntryWriter(moodTracker, oneNoteSync)

    suspend fun save(
        notes: String,
        affirmationCount: Int,
        completedAt: Long,
        moodLevel: Int? = null,
    ): Long {
        val normalizedMood = MoodScale.normalize(moodLevel)
        return writer.save(
            isValid = true,
            moodLevel = normalizedMood,
            atMillis = completedAt,
            source = MoodSource.AFFIRMATION_REVIEW,
            legacyTable = MoodTrackerRepository.TABLE_AFFIRMATION_REVIEW_SESSIONS,
            oneNoteType = OneNoteEntryType.AFFIRMATION_REVIEW,
            insert = {
                dao.insert(
                    AffirmationReviewSessionEntity(
                        notes = notes.trim(),
                        moodLevel = normalizedMood,
                        affirmationCount = affirmationCount.coerceAtLeast(0),
                        completedAt = completedAt,
                        listKind = kindKey,
                    ),
                )
            },
        ) ?: error("Affirmation review save rejected an always-valid entry")
    }

    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<AffirmationReviewSessionEntity>> =
        dao.observeInRange(kindKey, startMillis, endMillis)

    suspend fun getById(id: Long): AffirmationReviewSessionEntity? = dao.getById(id)

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
