package com.example.meditationparticles.domain.sessions

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object SessionStatsCalculator {
    private const val CALM_GOAL_MINUTES = 15

    fun computeStreak(completedAtMs: List<Long>, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        if (completedAtMs.isEmpty()) return 0

        val sessionDays = completedAtMs
            .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
            .toSet()

        val today = LocalDate.now(zoneId)
        var cursor = if (today in sessionDays) today else today.minusDays(1)
        var streak = 0

        while (cursor in sessionDays) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun totalSecondsOnDay(
        sessions: List<MeditationSession>,
        day: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int = sessions.filter { session ->
        Instant.ofEpochMilli(session.completedAt).atZone(zoneId).toLocalDate() == day
    }.sumOf { it.durationSeconds }

    fun calmMeterPercent(todaySeconds: Int): Int {
        val goalSeconds = CALM_GOAL_MINUTES * 60
        if (goalSeconds <= 0) return 0
        return ((todaySeconds * 100) / goalSeconds).coerceIn(0, 100)
    }

    fun calmMeterMessage(todaySeconds: Int, yesterdaySeconds: Int): String = when {
        todaySeconds <= 0 -> "Start a session to begin your calm meter."
        yesterdaySeconds <= 0 -> "Great start today — keep the momentum going."
        todaySeconds > yesterdaySeconds -> "You're significantly calmer than yesterday. Keep it up!"
        todaySeconds == yesterdaySeconds -> "Steady progress — matching yesterday's pace."
        else -> "Every session counts. Small steps add up."
    }

    fun buildHomeProgress(
        sessions: List<MeditationSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HomeProgress {
        if (sessions.isEmpty()) return HomeProgress.Empty

        val today = LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)
        val todaySeconds = totalSecondsOnDay(sessions, today, zoneId)
        val yesterdaySeconds = totalSecondsOnDay(sessions, yesterday, zoneId)

        return HomeProgress(
            streakDays = computeStreak(sessions.map { it.completedAt }, zoneId),
            calmMeterPercent = calmMeterPercent(todaySeconds),
            calmMeterMessage = calmMeterMessage(todaySeconds, yesterdaySeconds),
            todayMinutes = todaySeconds / 60,
            recentSessions = sessions.take(5),
        )
    }
}
