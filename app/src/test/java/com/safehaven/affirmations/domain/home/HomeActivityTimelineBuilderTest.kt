package com.safehaven.affirmations.domain.home

import com.safehaven.affirmations.domain.sessions.MeditationSession
import com.safehaven.affirmations.domain.sessions.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeActivityTimelineBuilderTest {
    @Test
    fun build_sortsNewestFirst() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = listOf(
                session(id = 1, completedAt = 1000L, title = "Older"),
                session(id = 2, completedAt = 3000L, title = "Newer"),
            ),
            reflections = emptyList(),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
        )

        assertEquals(listOf("Newer", "Older"), items.map { it.title })
    }

    @Test
    fun build_includesTextEntryWhenPresent() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = emptyList(),
            reflections = listOf(
                HomeActivityTimelineBuilder.TextEntryRow(
                    id = 7,
                    completedAt = 5000L,
                    label = "Meditation reflection",
                    text = "  Felt calm after breathing  ",
                ),
            ),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
        )

        assertEquals(1, items.size)
        assertEquals("Meditation reflection", items.first().title)
        assertEquals("Felt calm after breathing", items.first().textEntry)
    }

    @Test
    fun build_omitsBlankTextEntry() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = emptyList(),
            reflections = listOf(
                HomeActivityTimelineBuilder.TextEntryRow(
                    id = 1,
                    completedAt = 100L,
                    label = "Meditation reflection",
                    text = "   ",
                ),
            ),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
        )

        assertNull(items.first().textEntry)
    }

    @Test
    fun build_includesMoodLevelWhenPresent() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = emptyList(),
            reflections = listOf(
                HomeActivityTimelineBuilder.TextEntryRow(
                    id = 3,
                    completedAt = 5000L,
                    label = "Meditation reflection",
                    text = "Calm",
                    moodLevel = 4,
                ),
            ),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
        )

        assertEquals(4, items.first().moodLevel)
    }

    @Test
    fun build_respectsLimit() {
        val sessions = (1..60).map { index ->
            session(id = index.toLong(), completedAt = index.toLong(), title = "Session $index")
        }
        val items = HomeActivityTimelineBuilder.build(
            sessions = sessions,
            reflections = emptyList(),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
            limit = 10,
        )

        assertEquals(10, items.size)
        assertTrue(items.first().completedAt > items.last().completedAt)
    }

    @Test
    fun build_includesHeartsEntries() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = emptyList(),
            reflections = emptyList(),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
            heartsEntries = listOf(
                HomeActivityTimelineBuilder.TextEntryRow(
                    id = 12,
                    completedAt = 9000L,
                    label = "Delight Deposit",
                    text = "Shared a small joy today",
                    subtitle = "Alex",
                    moodLevel = 3,
                ),
            ),
        )

        assertEquals(1, items.size)
        assertEquals("Delight Deposit", items.first().title)
        assertEquals("Shared a small joy today", items.first().textEntry)
        assertEquals("Alex", items.first().subtitle)
        assertEquals(3, items.first().moodLevel)
    }

    @Test
    fun build_includesMoodCheckInsFromWidgetAndHome() {
        val items = HomeActivityTimelineBuilder.build(
            sessions = emptyList(),
            reflections = emptyList(),
            thoughtDumps = emptyList(),
            nvcEntries = emptyList(),
            refactoringEntries = emptyList(),
            centerOfGravityEntries = emptyList(),
            futureSelfMessages = emptyList(),
            affirmationReviews = emptyList(),
            moodCheckIns = listOf(
                HomeActivityTimelineBuilder.MoodCheckInRow(
                    id = 5,
                    completedAt = 8000L,
                    moodLevel = 4,
                    subtitle = "Widget · Green",
                ),
            ),
        )

        assertEquals(1, items.size)
        assertEquals("mood:5", items.first().id)
        assertEquals("Mood check-in", items.first().title)
        assertEquals("Widget · Green", items.first().subtitle)
        assertEquals(4, items.first().moodLevel)
        assertNull(items.first().textEntry)
    }

    private fun session(id: Long, completedAt: Long, title: String) = MeditationSession(
        id = id,
        type = SessionType.TIMER,
        title = title,
        detail = null,
        durationSeconds = 600,
        completedAt = completedAt,
    )
}
