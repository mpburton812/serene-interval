package com.safehaven.affirmations.domain.mood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

enum class MoodGraphPeriod {
    DAY,
    WEEK,
    MONTH,
    CALENDAR,
}

data class MoodPeriodBounds(
    val startMillis: Long,
    val endMillis: Long,
) {
    fun contains(millis: Long): Boolean = millis in startMillis until endMillis
}

fun moodPeriodBounds(
    period: MoodGraphPeriod,
    referenceMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): MoodPeriodBounds {
    val referenceDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
    val startDate = when (period) {
        MoodGraphPeriod.DAY -> referenceDate
        MoodGraphPeriod.WEEK -> {
            val weekFields = WeekFields.of(locale)
            referenceDate.with(weekFields.dayOfWeek(), 1)
        }
        MoodGraphPeriod.MONTH,
        MoodGraphPeriod.CALENDAR,
        -> referenceDate.withDayOfMonth(1)
    }
    val endDate = when (period) {
        MoodGraphPeriod.DAY -> startDate.plusDays(1)
        MoodGraphPeriod.WEEK -> startDate.plusWeeks(1)
        MoodGraphPeriod.MONTH,
        MoodGraphPeriod.CALENDAR,
        -> startDate.plusMonths(1)
    }
    return MoodPeriodBounds(
        startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )
}
