package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.SessionDao
import com.example.meditationparticles.data.local.SessionEntity
import com.example.meditationparticles.domain.sessions.HomeProgress
import com.example.meditationparticles.domain.sessions.MeditationSession
import com.example.meditationparticles.domain.sessions.SessionStatsCalculator
import com.example.meditationparticles.domain.sessions.SessionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository(
    private val dao: SessionDao,
) {
    fun observeHomeProgress(): Flow<HomeProgress> =
        dao.observeRecent(HOME_QUERY_LIMIT).map { entities ->
            SessionStatsCalculator.buildHomeProgress(entities.map { it.toDomain() })
        }

    suspend fun logBreathing(patternName: String, durationSeconds: Int) {
        if (durationSeconds < MIN_LOG_SECONDS) return
        dao.insert(
            SessionEntity(
                type = SessionType.BREATHING.name,
                title = patternName,
                detail = "Completed",
                durationSeconds = durationSeconds,
            ),
        )
    }

    suspend fun logTimer(targetMinutes: Int) {
        dao.insert(
            SessionEntity(
                type = SessionType.TIMER.name,
                title = "Meditation Timer",
                detail = "$targetMinutes min • Completed",
                durationSeconds = targetMinutes * 60,
            ),
        )
    }

    suspend fun logVisualization(title: String, durationSeconds: Int) {
        if (durationSeconds < MIN_LOG_SECONDS) return
        dao.insert(
            SessionEntity(
                type = SessionType.VISUALIZATION.name,
                title = title,
                detail = "Completed",
                durationSeconds = durationSeconds,
            ),
        )
    }

    private fun SessionEntity.toDomain(): MeditationSession = MeditationSession(
        id = id,
        type = runCatching { SessionType.valueOf(type) }.getOrDefault(SessionType.TIMER),
        title = title,
        detail = detail,
        durationSeconds = durationSeconds,
        completedAt = completedAt,
    )

    companion object {
        private const val HOME_QUERY_LIMIT = 200
        private const val MIN_LOG_SECONDS = 30
    }
}
