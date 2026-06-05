package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.CenterOfGravityEntryDao
import com.example.meditationparticles.data.local.FutureSelfMessageDao
import com.example.meditationparticles.data.local.MeditationReflectionDao
import com.example.meditationparticles.data.local.MoodEntryDao
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.data.local.NvcEntryDao
import com.example.meditationparticles.data.local.RefactoringEntryDao
import com.example.meditationparticles.data.local.ThoughtDumpDao
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodPeriodAverages
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import com.example.meditationparticles.domain.mood.moodPeriodBounds
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.ZoneId

class MoodTrackerRepository(
    private val moodEntryDao: MoodEntryDao,
    private val thoughtDumpDao: ThoughtDumpDao,
    private val meditationReflectionDao: MeditationReflectionDao,
    private val futureSelfMessageDao: FutureSelfMessageDao,
    private val refactoringEntryDao: RefactoringEntryDao,
    private val centerOfGravityEntryDao: CenterOfGravityEntryDao,
    private val nvcEntryDao: NvcEntryDao,
) {
    suspend fun record(
        source: MoodSource,
        level: Int,
        atMillis: Long = System.currentTimeMillis(),
        legacyTable: String? = null,
        legacyRowId: Long? = null,
    ): Long? {
        val moodLevel = MoodScale.normalize(level) ?: return null
        if (legacyTable != null && legacyRowId != null &&
            moodEntryDao.countLegacy(legacyTable, legacyRowId) > 0
        ) {
            return null
        }
        return moodEntryDao.insert(
            MoodEntryEntity(
                moodLevel = moodLevel,
                recordedAtMillis = atMillis,
                source = source.dbValue,
                legacyTable = legacyTable,
                legacyRowId = legacyRowId,
            ),
        )
    }

    suspend fun backfillFromLegacyIfNeeded(): Int {
        var inserted = 0
        inserted += backfillThoughtDumps()
        inserted += backfillMeditationReflections()
        inserted += backfillFutureSelfMessages()
        inserted += backfillRefactoringEntries()
        inserted += backfillCenterOfGravityEntries()
        inserted += backfillNvcEntries()
        return inserted
    }

    fun observeAverages(zoneId: ZoneId = ZoneId.systemDefault()): Flow<MoodPeriodAverages> {
        val dayBounds = moodPeriodBounds(MoodGraphPeriod.DAY, zoneId = zoneId)
        val weekBounds = moodPeriodBounds(MoodGraphPeriod.WEEK, zoneId = zoneId)
        val monthBounds = moodPeriodBounds(MoodGraphPeriod.MONTH, zoneId = zoneId)
        return combine(
            moodEntryDao.averageInRange(dayBounds.startMillis, dayBounds.endMillis),
            moodEntryDao.averageInRange(weekBounds.startMillis, weekBounds.endMillis),
            moodEntryDao.averageInRange(monthBounds.startMillis, monthBounds.endMillis),
        ) { day, week, month ->
            MoodPeriodAverages(day = day, week = week, month = month)
        }
    }

    fun observeEntriesForPeriod(
        period: MoodGraphPeriod,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<List<MoodEntryEntity>> {
        val bounds = moodPeriodBounds(period, zoneId = zoneId)
        return moodEntryDao.observeInRange(bounds.startMillis, bounds.endMillis)
    }

    private suspend fun backfillThoughtDumps(): Int {
        var inserted = 0
        for (entry in thoughtDumpDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            val source = when (entry.logType) {
                ToolkitLogType.ANXIETY_LOG.name -> MoodSource.ANXIETY_LOG
                else -> MoodSource.THOUGHT_DUMP
            }
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.createdAt,
                    source = source,
                    legacyTable = TABLE_THOUGHT_DUMPS,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun backfillMeditationReflections(): Int {
        var inserted = 0
        for (entry in meditationReflectionDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.completedAt,
                    source = MoodSource.MEDITATION_REFLECTION,
                    legacyTable = TABLE_MEDITATION_REFLECTIONS,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun backfillFutureSelfMessages(): Int {
        var inserted = 0
        for (entry in futureSelfMessageDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.createdAtMillis,
                    source = MoodSource.FUTURE_SELF,
                    legacyTable = TABLE_FUTURE_SELF_MESSAGES,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun backfillRefactoringEntries(): Int {
        var inserted = 0
        for (entry in refactoringEntryDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.createdAt,
                    source = MoodSource.REFACTORING,
                    legacyTable = TABLE_REFACTORING_ENTRIES,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun backfillCenterOfGravityEntries(): Int {
        var inserted = 0
        for (entry in centerOfGravityEntryDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.createdAt,
                    source = MoodSource.CENTER_OF_GRAVITY,
                    legacyTable = TABLE_CENTER_OF_GRAVITY_ENTRIES,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun backfillNvcEntries(): Int {
        var inserted = 0
        for (entry in nvcEntryDao.getAll()) {
            val moodLevel = entry.moodLevel ?: continue
            if (insertLegacyIfAbsent(
                    moodLevel = moodLevel,
                    recordedAtMillis = entry.createdAt,
                    source = MoodSource.NVC,
                    legacyTable = TABLE_NVC_ENTRIES,
                    legacyRowId = entry.id,
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun insertLegacyIfAbsent(
        moodLevel: Int,
        recordedAtMillis: Long,
        source: MoodSource,
        legacyTable: String,
        legacyRowId: Long,
    ): Boolean {
        if (moodEntryDao.countLegacy(legacyTable, legacyRowId) > 0) return false
        val normalized = MoodScale.normalize(moodLevel) ?: return false
        moodEntryDao.insert(
            MoodEntryEntity(
                moodLevel = normalized,
                recordedAtMillis = recordedAtMillis,
                source = source.dbValue,
                legacyTable = legacyTable,
                legacyRowId = legacyRowId,
            ),
        )
        return true
    }

    companion object {
        const val TABLE_THOUGHT_DUMPS = "thought_dumps"
        const val TABLE_MEDITATION_REFLECTIONS = "meditation_reflections"
        const val TABLE_FUTURE_SELF_MESSAGES = "future_self_messages"
        const val TABLE_REFACTORING_ENTRIES = "refactoring_entries"
        const val TABLE_CENTER_OF_GRAVITY_ENTRIES = "center_of_gravity_entries"
        const val TABLE_NVC_ENTRIES = "nvc_entries"
    }
}
