package com.example.meditationparticles.domain.mood

import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.home.HomeActivityItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MoodCalendarDayEventsTest {
    private val zoneId = ZoneId.of("America/New_York")
    private val date = LocalDate.of(2026, 6, 10)

    @Test
    fun build_combinesMoodEntriesAndActivitiesSortedByTime() {
        val moodEntry = MoodEntryEntity(
            id = 1,
            moodLevel = 3,
            recordedAtMillis = date.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            source = "test",
            legacyTable = null,
            legacyRowId = null,
        )
        val activity = HomeActivityItem(
            id = "session:1",
            completedAt = date.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli(),
            title = "Timer session",
            subtitle = "10 min",
        )

        val events = MoodCalendarDayEvents.build(
            date = date,
            moodEntries = listOf(moodEntry),
            activities = listOf(activity),
            zoneId = zoneId,
        )

        assertEquals(2, events.size)
        assertEquals("session:1", events.first().id)
        assertEquals("mood:1", events.last().id)
    }
}
