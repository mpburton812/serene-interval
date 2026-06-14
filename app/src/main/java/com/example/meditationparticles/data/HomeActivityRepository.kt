package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.domain.home.HomeActivityItem
import com.example.meditationparticles.domain.home.HomeActivityTimelineBuilder
import com.example.meditationparticles.domain.sessions.MeditationSession
import com.example.meditationparticles.domain.sessions.SessionType
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class HomeActivityRepository(
    private val database: SereneDatabase,
) {
    fun observeTimeline(limit: Int = HomeActivityTimelineBuilder.DEFAULT_LIMIT): Flow<List<HomeActivityItem>> {
        val sessionsFlow = database.sessionDao().observeRecent(limit).map { rows ->
            rows.map { entity ->
                MeditationSession(
                    id = entity.id,
                    type = runCatching { SessionType.valueOf(entity.type) }
                        .getOrDefault(SessionType.TIMER),
                    title = entity.title,
                    detail = entity.detail,
                    durationSeconds = entity.durationSeconds,
                    completedAt = entity.completedAt,
                )
            }
        }
        val reflectionsFlow = database.meditationReflectionDao().observeAll()
        val thoughtDumpsFlow = database.thoughtDumpDao().observeAll()
        val nvcFlow = database.nvcEntryDao().observeAll()
        val refactoringFlow = database.refactoringEntryDao().observeAll()
        val cogFlow = database.centerOfGravityEntryDao().observeAll()
        val futureSelfFlow = database.futureSelfMessageDao().observeAll()
        val affirmationReviewsFlow = database.affirmationReviewSessionDao().observeAll()

        val primaryFlow = combine(
            sessionsFlow,
            reflectionsFlow,
            thoughtDumpsFlow,
            nvcFlow,
            refactoringFlow,
        ) { sessions, reflections, thoughtDumps, nvc, refactoring ->
            TimelineSnapshot(
                sessions = sessions,
                reflections = reflections,
                thoughtDumps = thoughtDumps,
                nvc = nvc,
                refactoring = refactoring,
            )
        }

        val secondaryFlow = combine(cogFlow, futureSelfFlow, affirmationReviewsFlow) { cog, futureSelf, reviews ->
            Triple(cog, futureSelf, reviews)
        }

        return combine(primaryFlow, secondaryFlow) { primary, secondary ->
            val (cog, futureSelf, affirmationReviews) = secondary
            HomeActivityTimelineBuilder.build(
                sessions = primary.sessions,
                reflections = primary.reflections.map(::reflectionRow),
                thoughtDumps = primary.thoughtDumps.map(::thoughtDumpRow),
                nvcEntries = primary.nvc.map(::nvcRow),
                refactoringEntries = primary.refactoring.map(::refactoringRow),
                centerOfGravityEntries = cog.map(::cogRow),
                futureSelfMessages = futureSelf.map(::futureSelfRow),
                affirmationReviews = affirmationReviews.map(::affirmationReviewRow),
                limit = limit,
            )
        }.flowOn(Dispatchers.Default)
    }

    private data class TimelineSnapshot(
        val sessions: List<MeditationSession>,
        val reflections: List<com.example.meditationparticles.data.local.MeditationReflectionEntity>,
        val thoughtDumps: List<com.example.meditationparticles.data.local.ThoughtDumpEntity>,
        val nvc: List<com.example.meditationparticles.data.local.NvcEntryEntity>,
        val refactoring: List<com.example.meditationparticles.data.local.RefactoringEntryEntity>,
    )

    private fun reflectionRow(
        entity: com.example.meditationparticles.data.local.MeditationReflectionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Meditation reflection",
        text = entity.reflection,
        subtitle = formatMinutes(entity.durationSeconds),
    )

    private fun thoughtDumpRow(
        entity: com.example.meditationparticles.data.local.ThoughtDumpEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.createdAt,
        label = if (entity.logType == ToolkitLogType.ANXIETY_LOG.name) {
            "Anxiety log"
        } else {
            "Thought dump"
        },
        text = entity.content,
    )

    private fun nvcRow(entity: com.example.meditationparticles.data.local.NvcEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = "Non-Violent Communication",
            text = listOf(entity.observation, entity.feeling, entity.need, entity.request)
                .filter { it.isNotBlank() }
                .joinToString("\n\n"),
        )

    private fun refactoringRow(
        entity: com.example.meditationparticles.data.local.RefactoringEntryEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.createdAt,
        label = "Refactoring",
        text = listOf(
            entity.actualFacts,
            entity.interpretation,
            entity.explanation1,
            entity.explanation2,
            entity.explanation3,
        ).filter { it.isNotBlank() }.joinToString("\n\n"),
    )

    private fun cogRow(entity: com.example.meditationparticles.data.local.CenterOfGravityEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = "Center of Gravity",
            text = "${entity.thoughtsAndFeelings}\n\n${entity.bodyAndNeeds}".trim(),
        )

    private fun futureSelfRow(entity: com.example.meditationparticles.data.local.FutureSelfMessageEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAtMillis,
            label = "Future Self",
            text = entity.content,
        )

    private fun affirmationReviewRow(
        entity: com.example.meditationparticles.data.local.AffirmationReviewSessionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Affirmations review",
        text = entity.notes,
        subtitle = "${entity.affirmationCount} affirmations",
    )

    private fun formatMinutes(durationSeconds: Int): String? {
        if (durationSeconds <= 0) return null
        return "${durationSeconds / 60} min"
    }
}
