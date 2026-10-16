package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.onenote.OneNoteSyncRepository
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType

/**
 * Shared tail of a journal save: validate, insert, record mood, optionally enqueue OneNote.
 * Table shapes stay as they are; this only removes the copy-pasted write sequence.
 */
class JournalEntryWriter(
    private val moodTracker: MoodTrackerRepository,
    private val oneNoteSync: OneNoteSyncRepository? = null,
) {
    suspend fun save(
        isValid: Boolean,
        moodLevel: Int?,
        atMillis: Long,
        source: MoodSource,
        legacyTable: String,
        oneNoteType: OneNoteEntryType? = null,
        insert: suspend () -> Long,
    ): Long? {
        if (!isValid) return null
        val id = insert()
        recordMood(
            moodLevel = moodLevel,
            atMillis = atMillis,
            source = source,
            legacyTable = legacyTable,
            legacyRowId = id,
        )
        if (oneNoteType != null) {
            enqueue(oneNoteType, id)
        }
        return id
    }

    suspend fun enqueue(oneNoteType: OneNoteEntryType, id: Long) {
        oneNoteSync?.enqueueSync(oneNoteType, id)
    }

    suspend fun recordMood(
        moodLevel: Int?,
        atMillis: Long,
        source: MoodSource,
        legacyTable: String,
        legacyRowId: Long,
    ) {
        val level = MoodScale.normalize(moodLevel) ?: return
        moodTracker.record(
            source = source,
            level = level,
            atMillis = atMillis,
            legacyTable = legacyTable,
            legacyRowId = legacyRowId,
        )
    }
}
