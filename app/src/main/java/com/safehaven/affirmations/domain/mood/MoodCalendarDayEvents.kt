package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import com.safehaven.affirmations.domain.home.HomeActivityItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MoodCalendarEvent(
    val id: String,
    val title: String,
    val subtitle: String?,
    val detail: String?,
    val moodLevel: Int?,
    val completedAt: Long,
)

object MoodCalendarDayEvents {
    fun build(
        date: LocalDate,
        moodEntries: List<MoodEntryEntity>,
        activities: List<HomeActivityItem>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoodCalendarEvent> {
        val moodEvents = moodEntries
            .filter { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate() == date
            }
            .map { entry ->
                MoodCalendarEvent(
                    id = "mood:${entry.id}",
                    title = "Mood check-in",
                    subtitle = MoodScale.label(entry.moodLevel),
                    detail = null,
                    moodLevel = entry.moodLevel,
                    completedAt = entry.recordedAtMillis,
                )
            }

        val activityEvents = activities.map { activity ->
            MoodCalendarEvent(
                id = activity.id,
                title = activity.title,
                subtitle = activity.subtitle,
                detail = activity.textEntry,
                moodLevel = activity.moodLevel,
                completedAt = activity.completedAt,
            )
        }

        return (moodEvents + activityEvents).sortedByDescending { it.completedAt }
    }
}
