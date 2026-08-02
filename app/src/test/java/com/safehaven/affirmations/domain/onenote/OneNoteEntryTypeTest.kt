package com.safehaven.affirmations.domain.onenote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNoteEntryTypeTest {
    @Test
    fun affirmationReview_isRegisteredWithDisplayName() {
        val entryType = OneNoteEntryType.AFFIRMATION_REVIEW
        assertEquals("Affirmation Review", entryType.displayName)
        assertTrue(OneNoteEntryType.entries.contains(entryType))
    }
}
