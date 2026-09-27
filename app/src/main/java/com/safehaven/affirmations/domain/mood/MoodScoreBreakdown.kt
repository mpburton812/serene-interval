package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MoodScoreBucket(
    val label: String,
    val average: Double,
)

object MoodScoreBreakdown {
    fun heading(period: MoodGraphPeriod): String = when (period) {
        MoodGraphPeriod.DAY -> "Score by Day"
        MoodGraphPeriod.WEEK -> "Score by Week"
        MoodGraphPeriod.MONTH,
        MoodGraphPeriod.CALENDAR,
        -> "Score by Month"
        MoodGraphPeriod.YEAR -> "Score by Year"
    }

    fun buckets(
        period: MoodGraphPeriod,
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<MoodScoreBucket> {
        if (entries.isEmpty()) return emptyList()
        return when (period) {
            MoodGraphPeriod.DAY -> byHour(entries, zoneId, locale)
            MoodGraphPeriod.WEEK -> byDay(entries, zoneId, locale, pattern = "EEE d")
            MoodGraphPeriod.MONTH,
            MoodGraphPeriod.CALENDAR,
            -> byDay(entries, zoneId, locale, pattern = "MMM d")
            MoodGraphPeriod.YEAR -> byMonth(entries, zoneId, locale)
        }
    }

    private fun byHour(
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId,
        locale: Locale,
    ): List<MoodScoreBucket> {
        val formatter = DateTimeFormatter.ofPattern("h a", locale)
        return entries
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDateTime()
                    .withMinute(0).withSecond(0).withNano(0)
            }
            .toSortedMap()
            .map { (hour, hourEntries) ->
                MoodScoreBucket(
                    label = formatter.format(hour),
                    average = hourEntries.map { it.moodLevel.toDouble() }.average(),
                )
            }
    }

    private fun byDay(
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId,
        locale: Locale,
        pattern: String,
    ): List<MoodScoreBucket> {
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return entries
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate()
            }
            .toSortedMap()
            .map { (day, dayEntries) ->
                MoodScoreBucket(
                    label = formatter.format(day),
                    average = dayEntries.map { it.moodLevel.toDouble() }.average(),
                )
            }
    }

    private fun byMonth(
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId,
        locale: Locale,
    ): List<MoodScoreBucket> {
        val formatter = DateTimeFormatter.ofPattern("MMMM", locale)
        return entries
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate().withDayOfMonth(1)
            }
            .toSortedMap()
            .map { (month, monthEntries) ->
                MoodScoreBucket(
                    label = formatter.format(month),
                    average = monthEntries.map { it.moodLevel.toDouble() }.average(),
                )
            }
    }
}
