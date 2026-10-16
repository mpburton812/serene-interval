package com.safehaven.affirmations.ui.toolkit

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolkitDraftsTest {
    @Test
    fun toNvcEntry_trimsFieldsAndKeepsMood() {
        val entry = ToolkitUiState(
            nvcObservation = "  late  ",
            nvcFeeling = " tense ",
            nvcNeed = " respect ",
            nvcRequest = " call ",
            draftMoodLevel = 2,
        ).toNvcEntry()

        assertEquals("late", entry.observation)
        assertEquals("tense", entry.feeling)
        assertEquals("respect", entry.need)
        assertEquals("call", entry.request)
        assertEquals(2, entry.moodLevel)
    }

    @Test
    fun clearedNvcDraft_resetsStepAndFields() {
        val cleared = ToolkitUiState(
            nvcStepIndex = 3,
            nvcObservation = "x",
            nvcFeeling = "y",
            nvcNeed = "z",
            nvcRequest = "q",
            thoughtDumpText = "keep",
            draftMoodLevel = 3,
        ).clearedNvcDraft()

        assertEquals(0, cleared.nvcStepIndex)
        assertEquals("", cleared.nvcObservation)
        assertEquals("", cleared.nvcFeeling)
        assertEquals("", cleared.nvcNeed)
        assertEquals("", cleared.nvcRequest)
        assertEquals("keep", cleared.thoughtDumpText)
        assertEquals(3, cleared.draftMoodLevel)
    }
}
