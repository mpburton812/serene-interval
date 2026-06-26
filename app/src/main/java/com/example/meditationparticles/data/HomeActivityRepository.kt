package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.domain.home.HomeActivityItem
import com.example.meditationparticles.domain.home.HomeActivityTimelineBuilder
import com.example.meditationparticles.domain.sessions.MeditationSession
import com.example.meditationparticles.domain.sessions.SessionType
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import java.time.LocalDate
import java.time.ZoneId
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
        val heartsFlow = database.heartsEntryDao().observeAll()

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

        val secondaryFlow = combine(cogFlow, futureSelfFlow, affirmationReviewsFlow, heartsFlow) {
                cog, futureSelf, reviews, hearts ->
            TimelineSecondarySnapshot(
                centerOfGravity = cog,
                futureSelf = futureSelf,
                affirmationReviews = reviews,
                heartsEntries = hearts,
            )
        }

        return combine(primaryFlow, secondaryFlow) { primary, secondary ->
            HomeActivityTimelineBuilder.build(
                sessions = primary.sessions,
                reflections = primary.reflections.map(::reflectionRow),
                thoughtDumps = primary.thoughtDumps.map(::thoughtDumpRow),
                nvcEntries = primary.nvc.map(::nvcRow),
                refactoringEntries = primary.refactoring.map(::refactoringRow),
                centerOfGravityEntries = secondary.centerOfGravity.map(::cogRow),
                futureSelfMessages = secondary.futureSelf.map(::futureSelfRow),
                affirmationReviews = secondary.affirmationReviews.map(::affirmationReviewRow),
                heartsEntries = secondary.heartsEntries.map(::heartsRow),
                limit = limit,
            )
        }.flowOn(Dispatchers.Default)
    }

    fun observeActivitiesForDay(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int = 2_000,
    ): Flow<List<HomeActivityItem>> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return observeTimeline(limit = limit).map { items ->
            items
                .filter { it.completedAt in startMillis until endMillis }
                .sortedByDescending { it.completedAt }
        }
    }

    private data class TimelineSnapshot(
        val sessions: List<MeditationSession>,
        val reflections: List<com.example.meditationparticles.data.local.MeditationReflectionEntity>,
        val thoughtDumps: List<com.example.meditationparticles.data.local.ThoughtDumpEntity>,
        val nvc: List<com.example.meditationparticles.data.local.NvcEntryEntity>,
        val refactoring: List<com.example.meditationparticles.data.local.RefactoringEntryEntity>,
    )

    private data class TimelineSecondarySnapshot(
        val centerOfGravity: List<com.example.meditationparticles.data.local.CenterOfGravityEntryEntity>,
        val futureSelf: List<com.example.meditationparticles.data.local.FutureSelfMessageEntity>,
        val affirmationReviews: List<com.example.meditationparticles.data.local.AffirmationReviewSessionEntity>,
        val heartsEntries: List<com.example.meditationparticles.data.local.HeartsEntryEntity>,
    )

    private fun reflectionRow(
        entity: com.example.meditationparticles.data.local.MeditationReflectionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Meditation reflection",
        text = entity.reflection,
        subtitle = formatMinutes(entity.durationSeconds),
        moodLevel = entity.moodLevel,
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
        moodLevel = entity.moodLevel,
    )

    private fun nvcRow(entity: com.example.meditationparticles.data.local.NvcEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = "Non-Violent Communication",
            text = listOf(entity.observation, entity.feeling, entity.need, entity.request)
                .filter { it.isNotBlank() }
                .joinToString("\n\n"),
            moodLevel = entity.moodLevel,
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
        moodLevel = entity.moodLevel,
    )

    private fun cogRow(entity: com.example.meditationparticles.data.local.CenterOfGravityEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = "Center of Gravity",
            text = "${entity.thoughtsAndFeelings}\n\n${entity.bodyAndNeeds}".trim(),
            moodLevel = entity.moodLevel,
        )

    private fun futureSelfRow(entity: com.example.meditationparticles.data.local.FutureSelfMessageEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAtMillis,
            label = "Future Self",
            text = entity.content,
            moodLevel = entity.moodLevel,
        )

    private fun heartsRow(entity: com.example.meditationparticles.data.local.HeartsEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = heartsToolTitle(entity.toolId),
            text = buildHeartsTimelineText(entity),
            subtitle = entity.personName.takeIf { it.isNotBlank() },
            moodLevel = entity.moodLevel,
        )

    private fun buildHeartsTimelineText(entity: com.example.meditationparticles.data.local.HeartsEntryEntity): String =
        entity.stepValues().filter { it.isNotBlank() }.joinToString("\n\n")

    private fun heartsToolTitle(toolId: String): String {
        val id = runCatching { ToolkitToolId.valueOf(toolId) }.getOrNull()
        return when (id) {
            ToolkitToolId.DelightDeposit -> "Delight Deposit"
            ToolkitToolId.AttunementMap -> "Attunement Map"
            ToolkitToolId.RepairReconnect -> "Repair & Reconnect"
            ToolkitToolId.SecureSelfCheck -> "Secure Self Check"
            ToolkitToolId.AppreciationRitual -> "Appreciation Ritual"
            ToolkitToolId.NeedsBeforeNegotiation -> "Needs Before Negotiation"
            ToolkitToolId.AttachmentStorySnapshot -> "Attachment Story"
            else -> "HEARTS"
        }
    }

    private fun affirmationReviewRow(
        entity: com.example.meditationparticles.data.local.AffirmationReviewSessionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Affirmations review",
        text = entity.notes,
        subtitle = "${entity.affirmationCount} affirmations",
        moodLevel = entity.moodLevel,
    )

    private fun formatMinutes(durationSeconds: Int): String? {
        if (durationSeconds <= 0) return null
        return "${durationSeconds / 60} min"
    }
}
