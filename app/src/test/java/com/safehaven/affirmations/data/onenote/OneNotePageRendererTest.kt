package com.safehaven.affirmations.data.onenote

import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.local.AffirmationReviewSessionEntity
import com.safehaven.affirmations.data.local.MeditationReflectionEntity
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneNotePageRendererTest {
    @Test
    fun renderNvc_includesFieldsAndFooter() {
        val html = OneNotePageRenderer.renderNvc(
            NvcEntryEntity(
                id = 42L,
                observation = "When you arrived late",
                feeling = "Frustrated",
                need = "Predictability",
                request = "Please text if you're delayed",
            ),
        ).html

        assertTrue(html.contains("<h2 data-id=\"observation\">Observation</h2>"))
        assertTrue(html.contains("When you arrived late"))
        assertTrue(html.contains("Entry #42"))
        assertTrue(html.contains("<!DOCTYPE html>"))
    }

    @Test
    fun renderNvc_escapesHtmlInContent() {
        val html = OneNotePageRenderer.renderNvc(
            NvcEntryEntity(
                observation = "<script>alert(1)</script>",
                feeling = "",
                need = "",
                request = "",
            ),
        ).html

        assertFalse(html.contains("<script>"))
        assertTrue(html.contains("&lt;script&gt;"))
    }

    @Test
    fun renderRefactoring_includesAllFields() {
        val page = OneNotePageRenderer.renderRefactoring(
            RefactoringEntryEntity(
                id = 12L,
                interpretation = "They don't care",
                actualFacts = "They arrived at 8:20",
                explanation1 = "Traffic",
                explanation2 = "Overslept",
                explanation3 = "Forgot",
            ),
        )

        assertTrue(page.html.contains("Actual facts"))
        assertTrue(page.html.contains("They arrived at 8:20"))
        assertTrue(page.attachments.isEmpty())
    }

    @Test
    fun renderThoughtDump_usesSingleBody() {
        val html = OneNotePageRenderer.renderThoughtDump(
            ThoughtDumpEntity(content = "Buy milk\nCall dentist", logType = "THOUGHT_DUMP"),
        ).html

        assertTrue(html.contains("Buy milk<br/>Call dentist"))
    }

    @Test
    fun renderAnxietyLog_usesToolTitle() {
        val html = OneNotePageRenderer.renderAnxietyLog(
            ThoughtDumpEntity(content = "Tight chest", logType = "ANXIETY_LOG"),
        ).html

        assertTrue(html.contains("Anxiety Log"))
        assertTrue(html.contains("Tight chest"))
    }

    @Test
    fun renderFutureSelf_includesScheduledDate() {
        val html = OneNotePageRenderer.renderFutureSelf(
            FutureSelfMessageEntity(
                content = "You’ve got this",
                scheduledAtMillis = 1_735_689_600_000L,
            ),
        ).html

        assertTrue(html.contains("Deliver on:"))
        assertTrue(html.contains("You’ve got this"))
    }

    @Test
    fun renderCenterOfGravity_includesBothSections() {
        val html = OneNotePageRenderer.renderCenterOfGravity(
            CenterOfGravityEntryEntity(
                thoughtsAndFeelings = "I feel overlooked",
                bodyAndNeeds = "Tight shoulders; need space",
            ),
        ).html

        assertTrue(html.contains("Thoughts and feelings"))
        assertTrue(html.contains("Body and needs"))
    }

    @Test
    fun renderAffirmationReview_includesCountAndNotes() {
        val page = OneNotePageRenderer.renderAffirmationReview(
            AffirmationReviewSessionEntity(
                id = 12L,
                notes = "Felt grounded",
                moodLevel = 4,
                affirmationCount = 8,
                completedAt = 1_735_689_600_000L,
            ),
        )

        assertTrue(page.html.contains("Affirmations reviewed"))
        assertTrue(page.html.contains("8"))
        assertTrue(page.html.contains("Felt grounded"))
    }

    @Test
    fun renderMeditationReflection_includesReflectionText() {
        val page = OneNotePageRenderer.renderMeditationReflection(
            MeditationReflectionEntity(
                id = 99L,
                reflection = "Calm and focused",
                durationSeconds = 600,
                completedAt = 1_735_689_600_000L,
            ),
        )

        assertTrue(page.html.contains("Calm and focused"))
        assertTrue(page.attachments.isEmpty())
    }

    @Test
    fun renderNvc_moodUsesFourPointScaleWithLabel() {
        val html = OneNotePageRenderer.renderNvc(
            NvcEntryEntity(
                observation = "Rain",
                feeling = "Calm",
                need = "Rest",
                request = "Pause",
                moodLevel = 3,
            ),
        ).html

        assertTrue(html.contains("Mood:</strong> 3/4 (Blue)"))
    }

    @Test
    fun renderNvc_legacyMoodFiveRendersAsFour() {
        val html = OneNotePageRenderer.renderNvc(
            NvcEntryEntity(
                observation = "Done",
                feeling = "Good",
                need = "Joy",
                request = "Celebrate",
                moodLevel = 5,
            ),
        ).html

        assertTrue(html.contains("Mood:</strong> 4/4 (Green)"))
    }

    @Test
    fun render_dispatchesByEntryType() {
        val entry = ThoughtDumpEntity(id = 7L, content = "Hello", logType = "THOUGHT_DUMP")
        val html = OneNotePageRenderer.render(OneNoteEntryType.THOUGHT_DUMP, entry).html
        assertTrue(html.contains("Hello"))
    }
}
