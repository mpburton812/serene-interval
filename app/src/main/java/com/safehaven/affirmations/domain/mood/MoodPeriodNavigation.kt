package com.safehaven.affirmations.domain.mood

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

const val MAX_FUTURE_PERIOD_OFFSET = 0

fun canShiftPeriodForward(offset: Int): Boolean = offset < MAX_FUTURE_PERIOD_OFFSET

fun clampPeriodOffset(offset: Int): Int = minOf(offset, MAX_FUTURE_PERIOD_OFFSET)

fun effectiveGraphEndMillis(
    period: MoodGraphPeriod,
    offset: Int,
    fullEndMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
    nowMillis: Long = System.currentTimeMillis(),
): Long {
    if (offset < MAX_FUTURE_PERIOD_OFFSET) return fullEndMillis
    return minOf(fullEndMillis, nowMillis)
}

fun periodReferenceMillis(
    period: MoodGraphPeriod,
    offset: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): Long {
    val today = LocalDate.now(zoneId)
    val referenceDate = when (period) {
        MoodGraphPeriod.DAY -> today.plusDays(offset.toLong())
        MoodGraphPeriod.WEEK -> today.plusWeeks(offset.toLong())
        MoodGraphPeriod.MONTH,
        MoodGraphPeriod.CALENDAR,
        -> today.plusMonths(offset.toLong())
    }
    return referenceDate.atStartOfDay(zoneId).plusHours(12).toInstant().toEpochMilli()
}

fun moodPeriodTitle(
    period: MoodGraphPeriod,
    offset: Int,
    zoneId: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String {
    val referenceMillis = periodReferenceMillis(period, offset, zoneId, locale)
    val referenceDate = Instant.ofEpochMilli(referenceMillis).atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return when (period) {
        MoodGraphPeriod.DAY -> when {
            offset == 0 -> "Today"
            offset == -1 -> "Yesterday"
            offset == 1 -> "Tomorrow"
            else -> DateTimeFormatter.ofPattern("EEE, MMM d", locale).format(referenceDate)
        }
        MoodGraphPeriod.WEEK -> {
            val weekFields = WeekFields.of(locale)
            val weekStart = referenceDate.with(weekFields.dayOfWeek(), 1)
            val weekEnd = weekStart.plusDays(6)
            val formatter = DateTimeFormatter.ofPattern("MMM d", locale)
            when {
                offset == 0 && today in weekStart..weekEnd -> "This Week"
                else -> "${formatter.format(weekStart)} – ${formatter.format(weekEnd)}"
            }
        }
        MoodGraphPeriod.MONTH,
        MoodGraphPeriod.CALENDAR,
        -> {
            val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
            when {
                offset == 0 &&
                    referenceDate.month == today.month &&
                    referenceDate.year == today.year -> "This Month"
                else -> formatter.format(referenceDate.withDayOfMonth(1))
            }
        }
    }
}
