package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.SereneDatabase
import com.safehaven.affirmations.domain.home.HomeActivityItem
import com.safehaven.affirmations.domain.home.HomeActivityTimelineBuilder
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.sessions.MeditationSession
import com.safehaven.affirmations.domain.sessions.SessionType
import com.safehaven.affirmations.domain.toolkit.ToolkitLogType
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
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
            rows.map { entity -> entity.toMeditationSession() }
        }
        return observeTimelineFrom(sessionsFlow, limit)
    }

    /**
     * Day-scoped timeline that queries sessions by range so historical days
     * (e.g. July 31) still show past meditations even when the global recent
     * timeline is truncated.
     */
    fun observeActivitiesForDay(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        limit: Int = 2_000,
    ): Flow<List<HomeActivityItem>> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val sessionsFlow = database.sessionDao().observeInRange(startMillis, endMillis).map { rows ->
            rows.map { entity -> entity.toMeditationSession() }
        }
        return observeTimelineFrom(sessionsFlow, limit).map { items ->
            items
                .filter { it.completedAt in startMillis until endMillis }
                .sortedByDescending { it.completedAt }
        }
    }

    private fun observeTimelineFrom(
        sessionsFlow: Flow<List<MeditationSession>>,
        limit: Int,
    ): Flow<List<HomeActivityItem>> {
        val reflectionsFlow = database.meditationReflectionDao().observeAll()
        val thoughtDumpsFlow = database.thoughtDumpDao().observeAll()
        val nvcFlow = database.nvcEntryDao().observeAll()
        val refactoringFlow = database.refactoringEntryDao().observeAll()
        val cogFlow = database.centerOfGravityEntryDao().observeAll()
        val futureSelfFlow = database.futureSelfMessageDao().observeAll()
        val affirmationReviewsFlow = database.affirmationReviewSessionDao().observeAllKinds()
        val heartsFlow = database.heartsEntryDao().observeAll()
        val moodCheckInsFlow = database.moodEntryDao().observeStandalone()

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

        return combine(primaryFlow, secondaryFlow, moodCheckInsFlow) { primary, secondary, moodCheckIns ->
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
                moodCheckIns = moodCheckIns.map(::moodCheckInRow),
                limit = limit,
            )
        }.flowOn(Dispatchers.Default)
    }

    private fun com.safehaven.affirmations.data.local.SessionEntity.toMeditationSession(): MeditationSession =
        MeditationSession(
            id = id,
            type = runCatching { SessionType.valueOf(type) }
                .getOrDefault(SessionType.TIMER),
            title = title,
            detail = detail,
            durationSeconds = durationSeconds,
            completedAt = completedAt,
        )

    private data class TimelineSnapshot(
        val sessions: List<MeditationSession>,
        val reflections: List<com.safehaven.affirmations.data.local.MeditationReflectionEntity>,
        val thoughtDumps: List<com.safehaven.affirmations.data.local.ThoughtDumpEntity>,
        val nvc: List<com.safehaven.affirmations.data.local.NvcEntryEntity>,
        val refactoring: List<com.safehaven.affirmations.data.local.RefactoringEntryEntity>,
    )

    private data class TimelineSecondarySnapshot(
        val centerOfGravity: List<com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity>,
        val futureSelf: List<com.safehaven.affirmations.data.local.FutureSelfMessageEntity>,
        val affirmationReviews: List<com.safehaven.affirmations.data.local.AffirmationReviewSessionEntity>,
        val heartsEntries: List<com.safehaven.affirmations.data.local.HeartsEntryEntity>,
    )

    private fun reflectionRow(
        entity: com.safehaven.affirmations.data.local.MeditationReflectionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Meditation reflection",
        text = entity.reflection,
        subtitle = formatMinutes(entity.durationSeconds),
        moodLevel = entity.moodLevel,
    )

    private fun thoughtDumpRow(
        entity: com.safehaven.affirmations.data.local.ThoughtDumpEntity,
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

    private fun nvcRow(entity: com.safehaven.affirmations.data.local.NvcEntryEntity) =
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
        entity: com.safehaven.affirmations.data.local.RefactoringEntryEntity,
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

    private fun cogRow(entity: com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = "Center of Gravity",
            text = "${entity.thoughtsAndFeelings}\n\n${entity.bodyAndNeeds}".trim(),
            moodLevel = entity.moodLevel,
        )

    private fun futureSelfRow(entity: com.safehaven.affirmations.data.local.FutureSelfMessageEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAtMillis,
            label = "Future Self",
            text = entity.content,
            moodLevel = entity.moodLevel,
        )

    private fun heartsRow(entity: com.safehaven.affirmations.data.local.HeartsEntryEntity) =
        HomeActivityTimelineBuilder.TextEntryRow(
            id = entity.id,
            completedAt = entity.createdAt,
            label = heartsToolTitle(entity.toolId),
            text = buildHeartsTimelineText(entity),
            subtitle = entity.personName.takeIf { it.isNotBlank() },
            moodLevel = entity.moodLevel,
        )

    private fun buildHeartsTimelineText(entity: com.safehaven.affirmations.data.local.HeartsEntryEntity): String =
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
        entity: com.safehaven.affirmations.data.local.AffirmationReviewSessionEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.completedAt,
        label = "Affirmations review",
        text = entity.notes,
        subtitle = "${entity.affirmationCount} affirmations",
        moodLevel = entity.moodLevel,
    )

    private fun moodCheckInRow(
        entity: com.safehaven.affirmations.data.local.MoodEntryEntity,
    ) = HomeActivityTimelineBuilder.TextEntryRow(
        id = entity.id,
        completedAt = entity.recordedAtMillis,
        label = moodCheckInLabel(entity.source),
        text = "",
        subtitle = MoodScale.label(entity.moodLevel),
        moodLevel = entity.moodLevel,
    )

    private fun moodCheckInLabel(source: String): String = when (MoodSource.fromDbValue(source)) {
        MoodSource.WIDGET -> "Mood check-in (widget)"
        MoodSource.HOME_SCREEN -> "Mood check-in"
        else -> "Mood check-in"
    }

    private fun formatMinutes(durationSeconds: Int): String? {
        if (durationSeconds <= 0) return null
        return "${durationSeconds / 60} min"
    }
}
