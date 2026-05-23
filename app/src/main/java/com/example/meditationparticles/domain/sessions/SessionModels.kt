package com.example.meditationparticles.domain.sessions

enum class SessionType {
    BREATHING,
    TIMER,
    VISUALIZATION,
}

data class MeditationSession(
    val id: Long,
    val type: SessionType,
    val title: String,
    val detail: String?,
    val durationSeconds: Int,
    val completedAt: Long,
)

data class HomeProgress(
    val streakDays: Int,
    val calmMeterPercent: Int,
    val calmMeterMessage: String,
    val todayMinutes: Int,
    val recentSessions: List<MeditationSession>,
) {
    companion object {
        val Empty = HomeProgress(
            streakDays = 0,
            calmMeterPercent = 0,
            calmMeterMessage = "Start a session to begin your calm meter.",
            todayMinutes = 0,
            recentSessions = emptyList(),
        )
    }
}
