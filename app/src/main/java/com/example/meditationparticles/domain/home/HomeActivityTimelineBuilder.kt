package com.example.meditationparticles.domain.home

import com.example.meditationparticles.domain.sessions.MeditationSession
import com.example.meditationparticles.domain.sessions.SessionType

object HomeActivityTimelineBuilder {
    fun build(
        sessions: List<MeditationSession>,
        reflections: List<TextEntryRow>,
        thoughtDumps: List<TextEntryRow>,
        nvcEntries: List<TextEntryRow>,
        refactoringEntries: List<TextEntryRow>,
        centerOfGravityEntries: List<TextEntryRow>,
        futureSelfMessages: List<TextEntryRow>,
        affirmationReviews: List<TextEntryRow>,
        limit: Int = DEFAULT_LIMIT,
    ): List<HomeActivityItem> {
        val items = buildList {
            sessions.forEach { session ->
                add(
                    HomeActivityItem(
                        id = "session:${session.id}",
                        completedAt = session.completedAt,
                        title = session.title,
                        subtitle = session.detail ?: formatDuration(session.durationSeconds),
                        textEntry = null,
                    ),
                )
            }
            reflections.forEach { row ->
                add(row.toItem(prefix = "reflection", title = "Meditation reflection"))
            }
            thoughtDumps.forEach { row ->
                add(row.toItem(prefix = "thought_dump", title = row.label))
            }
            nvcEntries.forEach { row ->
                add(row.toItem(prefix = "nvc", title = "Non-Violent Communication"))
            }
            refactoringEntries.forEach { row ->
                add(row.toItem(prefix = "refactoring", title = "Refactoring"))
            }
            centerOfGravityEntries.forEach { row ->
                add(row.toItem(prefix = "cog", title = "Center of Gravity"))
            }
            futureSelfMessages.forEach { row ->
                add(row.toItem(prefix = "future_self", title = "Future Self"))
            }
            affirmationReviews.forEach { row ->
                add(row.toItem(prefix = "affirmation_review", title = "Affirmations review"))
            }
        }
        return items
            .sortedByDescending { it.completedAt }
            .take(limit)
    }

    data class TextEntryRow(
        val id: Long,
        val completedAt: Long,
        val label: String,
        val text: String,
        val subtitle: String? = null,
    )

    private fun TextEntryRow.toItem(prefix: String, title: String): HomeActivityItem {
        val trimmed = text.trim()
        return HomeActivityItem(
            id = "$prefix:$id",
            completedAt = completedAt,
            title = title,
            subtitle = subtitle,
            textEntry = trimmed.takeIf { it.isNotEmpty() },
        )
    }

    private fun formatDuration(durationSeconds: Int): String? {
        if (durationSeconds <= 0) return null
        val minutes = durationSeconds / 60
        return if (minutes > 0) "$minutes min" else "${durationSeconds}s"
    }

    fun sessionTypeLabel(type: SessionType): String = when (type) {
        SessionType.BREATHING -> "Breathing"
        SessionType.TIMER -> "Meditation timer"
        SessionType.VISUALIZATION -> "Visualization"
    }

    const val DEFAULT_LIMIT = 50
}
