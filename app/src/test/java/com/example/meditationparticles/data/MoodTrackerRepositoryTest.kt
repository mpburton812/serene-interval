package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.data.local.MoodEntryDao
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.mood.MoodSource
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodTrackerRepositoryTest {
    @Test
    fun record_normalizesMoodLevelBeforeInsert() = runTest {
        val moodDao = FakeMoodEntryDao()
        val repository = createRepository(moodDao = moodDao)

        repository.record(MoodSource.HOME_SCREEN, level = 5)

        assertEquals(4, moodDao.entries.single().moodLevel)
        assertEquals(MoodSource.HOME_SCREEN.dbValue, moodDao.entries.single().source)
    }

    @Test
    fun backfillFromLegacyIfNeeded_importsAllLegacyTablesAndNormalizesMood() = runTest {
        val moodDao = FakeMoodEntryDao()
        val thoughtDumpDao = FakeThoughtDumpDao(
            listOf(
                ThoughtDumpEntity(
                    id = 1L,
                    content = "thought",
                    logType = ToolkitLogType.THOUGHT_DUMP.name,
                    moodLevel = 3,
                    createdAt = 1_000L,
                ),
                ThoughtDumpEntity(
                    id = 2L,
                    content = "anxiety",
                    logType = ToolkitLogType.ANXIETY_LOG.name,
                    moodLevel = 5,
                    createdAt = 2_000L,
                ),
                ThoughtDumpEntity(
                    id = 3L,
                    content = "no mood",
                    logType = ToolkitLogType.THOUGHT_DUMP.name,
                    moodLevel = null,
                    createdAt = 3_000L,
                ),
            ),
        )
        val repository = createRepository(
            moodDao = moodDao,
            thoughtDumpDao = thoughtDumpDao,
            meditationReflectionDao = FakeMeditationReflectionDao(
                listOf(
                    MeditationReflectionEntity(
                        id = 10L,
                        reflection = "calm",
                        moodLevel = 2,
                        durationSeconds = 600,
                        completedAt = 10_000L,
                    ),
                ),
            ),
            futureSelfMessageDao = FakeFutureSelfMessageDao(
                listOf(
                    FutureSelfMessageEntity(
                        id = 20L,
                        content = "future",
                        moodLevel = 4,
                        scheduledAtMillis = 20_000L,
                        createdAtMillis = 20_500L,
                    ),
                ),
            ),
            refactoringEntryDao = FakeRefactoringEntryDao(
                listOf(
                    RefactoringEntryEntity(
                        id = 30L,
                        interpretation = "story",
                        actualFacts = "facts",
                        explanation1 = "e1",
                        explanation2 = "e2",
                        explanation3 = "e3",
                        moodLevel = 1,
                        createdAt = 30_000L,
                    ),
                ),
            ),
            centerOfGravityEntryDao = FakeCenterOfGravityEntryDao(
                listOf(
                    CenterOfGravityEntryEntity(
                        id = 40L,
                        thoughtsAndFeelings = "feel",
                        bodyAndNeeds = "needs",
                        moodLevel = 3,
                        createdAt = 40_000L,
                    ),
                ),
            ),
            nvcEntryDao = FakeNvcEntryDao(
                listOf(
                    NvcEntryEntity(
                        id = 50L,
                        observation = "obs",
                        feeling = "feel",
                        need = "need",
                        request = "req",
                        moodLevel = 2,
                        createdAt = 50_000L,
                    ),
                ),
            ),
        )

        val inserted = repository.backfillFromLegacyIfNeeded()

        assertEquals(7, inserted)
        assertEquals(7, moodDao.entries.size)
        assertEquals(
            MoodSource.THOUGHT_DUMP.dbValue,
            moodDao.entries.find { it.legacyRowId == 1L }?.source,
        )
        assertEquals(
            MoodSource.ANXIETY_LOG.dbValue,
            moodDao.entries.find { it.legacyRowId == 2L }?.source,
        )
        assertEquals(4, moodDao.entries.find { it.legacyRowId == 2L }?.moodLevel)
        assertEquals(
            MoodTrackerRepository.TABLE_MEDITATION_REFLECTIONS,
            moodDao.entries.find { it.legacyRowId == 10L }?.legacyTable,
        )
        assertEquals(10_000L, moodDao.entries.find { it.legacyRowId == 10L }?.recordedAtMillis)
        assertEquals(20_500L, moodDao.entries.find { it.legacyRowId == 20L }?.recordedAtMillis)
    }

    @Test
    fun record_withLegacyLinkage_skipsDuplicateLegacyRow() = runTest {
        val moodDao = FakeMoodEntryDao()
        val repository = createRepository(moodDao = moodDao)

        repository.record(
            source = MoodSource.THOUGHT_DUMP,
            level = 3,
            atMillis = 1_000L,
            legacyTable = MoodTrackerRepository.TABLE_THOUGHT_DUMPS,
            legacyRowId = 42L,
        )
        repository.record(
            source = MoodSource.THOUGHT_DUMP,
            level = 4,
            atMillis = 2_000L,
            legacyTable = MoodTrackerRepository.TABLE_THOUGHT_DUMPS,
            legacyRowId = 42L,
        )

        assertEquals(1, moodDao.entries.size)
        assertEquals(3, moodDao.entries.single().moodLevel)
    }

    @Test
    fun backfillFromLegacyIfNeeded_isIdempotentPerLegacyRow() = runTest {
        val moodDao = FakeMoodEntryDao()
        val thoughtDumpDao = FakeThoughtDumpDao(
            listOf(
                ThoughtDumpEntity(
                    id = 1L,
                    content = "thought",
                    logType = ToolkitLogType.THOUGHT_DUMP.name,
                    moodLevel = 3,
                    createdAt = 1_000L,
                ),
            ),
        )
        val repository = createRepository(moodDao = moodDao, thoughtDumpDao = thoughtDumpDao)

        assertEquals(1, repository.backfillFromLegacyIfNeeded())
        assertEquals(0, repository.backfillFromLegacyIfNeeded())
        assertEquals(1, moodDao.entries.size)
    }

    private fun createRepository(
        moodDao: FakeMoodEntryDao = FakeMoodEntryDao(),
        thoughtDumpDao: FakeThoughtDumpDao = FakeThoughtDumpDao(),
        meditationReflectionDao: FakeMeditationReflectionDao = FakeMeditationReflectionDao(),
        futureSelfMessageDao: FakeFutureSelfMessageDao = FakeFutureSelfMessageDao(),
        refactoringEntryDao: FakeRefactoringEntryDao = FakeRefactoringEntryDao(),
        centerOfGravityEntryDao: FakeCenterOfGravityEntryDao = FakeCenterOfGravityEntryDao(),
        nvcEntryDao: FakeNvcEntryDao = FakeNvcEntryDao(),
    ): MoodTrackerRepository = MoodTrackerRepository(
        moodEntryDao = moodDao,
        thoughtDumpDao = thoughtDumpDao,
        meditationReflectionDao = meditationReflectionDao,
        futureSelfMessageDao = futureSelfMessageDao,
        refactoringEntryDao = refactoringEntryDao,
        centerOfGravityEntryDao = centerOfGravityEntryDao,
        nvcEntryDao = nvcEntryDao,
    )

    private class FakeMoodEntryDao : MoodEntryDao {
        val entries = mutableListOf<MoodEntryEntity>()
        private val flow = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insert(entity: MoodEntryEntity): Long {
            val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
            val stored = entity.copy(id = id)
            entries += stored
            flow.value = entries.sortedByDescending { it.recordedAtMillis }
            return id
        }

        override fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<MoodEntryEntity>> =
            flow.map { list ->
                list.filter { it.recordedAtMillis in startMillis until endMillis }
            }

        override fun averageInRange(startMillis: Long, endMillis: Long): Flow<Double?> =
            observeInRange(startMillis, endMillis).map { list ->
                list.takeIf { it.isNotEmpty() }?.map { it.moodLevel.toDouble() }?.average()
            }

        override suspend fun countLegacy(legacyTable: String, legacyRowId: Long): Int =
            entries.count { it.legacyTable == legacyTable && it.legacyRowId == legacyRowId }

        override suspend fun getAll(): List<MoodEntryEntity> = entries.toList()

        override suspend fun clearAll() {
            entries.clear()
            flow.value = emptyList()
        }
    }

    private class FakeThoughtDumpDao(
        private val entries: List<ThoughtDumpEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.ThoughtDumpDao {
        override suspend fun getAll(): List<ThoughtDumpEntity> = entries

        override fun observeByType(logType: String) = throw UnsupportedOperationException()

        override fun observeLatest() = throw UnsupportedOperationException()

        override suspend fun insert(entity: ThoughtDumpEntity) = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()

        override suspend fun clearAll() = throw UnsupportedOperationException()
    }

    private class FakeMeditationReflectionDao(
        private val entries: List<MeditationReflectionEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.MeditationReflectionDao {
        override fun observeAll() = throw UnsupportedOperationException()

        override suspend fun getAll(): List<MeditationReflectionEntity> = entries

        override suspend fun insert(entity: MeditationReflectionEntity) = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class FakeFutureSelfMessageDao(
        private val entries: List<FutureSelfMessageEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.FutureSelfMessageDao {
        override suspend fun getAll(): List<FutureSelfMessageEntity> = entries

        override fun observeAll() = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun getPendingAfter(nowMillis: Long) = throw UnsupportedOperationException()

        override suspend fun getOverdueUndelivered(nowMillis: Long) = throw UnsupportedOperationException()

        override suspend fun insert(entity: FutureSelfMessageEntity) = throw UnsupportedOperationException()

        override suspend fun update(entity: FutureSelfMessageEntity) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()

        override suspend fun markDelivered(id: Long) = throw UnsupportedOperationException()
    }

    private class FakeRefactoringEntryDao(
        private val entries: List<RefactoringEntryEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.RefactoringEntryDao {
        override suspend fun getAll(): List<RefactoringEntryEntity> = entries

        override fun observeAll() = throw UnsupportedOperationException()

        override suspend fun insert(entity: RefactoringEntryEntity) = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class FakeCenterOfGravityEntryDao(
        private val entries: List<CenterOfGravityEntryEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.CenterOfGravityEntryDao {
        override suspend fun getAll(): List<CenterOfGravityEntryEntity> = entries

        override fun observeAll() = throw UnsupportedOperationException()

        override suspend fun insert(entity: CenterOfGravityEntryEntity) = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class FakeNvcEntryDao(
        private val entries: List<NvcEntryEntity> = emptyList(),
    ) : com.example.meditationparticles.data.local.NvcEntryDao {
        override suspend fun getAll(): List<NvcEntryEntity> = entries

        override fun observeAll() = throw UnsupportedOperationException()

        override suspend fun insert(entity: NvcEntryEntity) = throw UnsupportedOperationException()

        override suspend fun getById(id: Long) = throw UnsupportedOperationException()

        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }
}
