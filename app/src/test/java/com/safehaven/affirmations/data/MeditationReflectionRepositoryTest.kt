package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
import com.safehaven.affirmations.data.local.MeditationReflectionDao
import com.safehaven.affirmations.data.local.MeditationReflectionEntity
import com.safehaven.affirmations.data.local.MoodEntryDao
import com.safehaven.affirmations.data.local.MoodEntryEntity
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.domain.mood.MoodSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationReflectionRepositoryTest {
    @Test
    fun save_persistsReflectionAndRecordsMood() = runTest {
        val dao = FakeMeditationReflectionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = MeditationReflectionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        val id = repository.save(
            reflection = "  Felt calm  ",
            durationSeconds = 600,
            completedAt = 9_000L,
            moodLevel = 4,
        )

        assertEquals(1L, id)
        val saved = dao.entities.single()
        assertEquals("Felt calm", saved.reflection)
        assertEquals(600, saved.durationSeconds)
        assertEquals(9_000L, saved.completedAt)
        assertEquals(4, saved.moodLevel)

        val moodEntry = moodDao.entries.single()
        assertEquals(MoodSource.MEDITATION_REFLECTION.dbValue, moodEntry.source)
        assertEquals(4, moodEntry.moodLevel)
        assertEquals(MoodTrackerRepository.TABLE_MEDITATION_REFLECTIONS, moodEntry.legacyTable)
        assertEquals(1L, moodEntry.legacyRowId)
    }

    @Test
    fun save_moodOnly_recordsMoodWithoutReflectionText() = runTest {
        val dao = FakeMeditationReflectionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = MeditationReflectionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        val id = repository.save(
            reflection = "   ",
            durationSeconds = 300,
            completedAt = 4_000L,
            moodLevel = 2,
        )

        assertEquals(1L, id)
        val saved = dao.entities.single()
        assertEquals("", saved.reflection)
        assertEquals(2, saved.moodLevel)
        assertEquals(2, moodDao.entries.single().moodLevel)
        assertEquals(MoodSource.MEDITATION_REFLECTION.dbValue, moodDao.entries.single().source)
    }

    @Test
    fun save_withoutMoodOrText_skipsInsert() = runTest {
        val dao = FakeMeditationReflectionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = MeditationReflectionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        val id = repository.save(
            reflection = "  ",
            durationSeconds = 300,
            completedAt = 1_000L,
            moodLevel = null,
        )

        assertNull(id)
        assertTrue(dao.entities.isEmpty())
        assertTrue(moodDao.entries.isEmpty())
    }

    @Test
    fun save_withoutMood_skipsMoodTracker() = runTest {
        val dao = FakeMeditationReflectionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = MeditationReflectionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        repository.save(
            reflection = "Quiet mind",
            durationSeconds = 120,
            completedAt = 1_000L,
            moodLevel = null,
        )

        assertTrue(moodDao.entries.isEmpty())
        assertEquals("Quiet mind", dao.entities.single().reflection)
    }

    private fun moodTracker(moodDao: FakeMoodEntryDao): MoodTrackerRepository =
        MoodTrackerRepository(
            moodEntryDao = moodDao,
            thoughtDumpDao = EmptyThoughtDumpDao(),
            meditationReflectionDao = EmptyMeditationReflectionDao(),
            futureSelfMessageDao = EmptyFutureSelfMessageDao(),
            refactoringEntryDao = EmptyRefactoringEntryDao(),
            centerOfGravityEntryDao = EmptyCenterOfGravityEntryDao(),
            nvcEntryDao = EmptyNvcEntryDao(),
        )

    private class FakeMeditationReflectionDao : MeditationReflectionDao {
        val entities = mutableListOf<MeditationReflectionEntity>()
        private var nextId = 1L

        override fun observeAll() = kotlinx.coroutines.flow.flowOf(entities.toList())

        override suspend fun getAll(): List<MeditationReflectionEntity> = entities.toList()

        override suspend fun insert(entity: MeditationReflectionEntity): Long {
            val id = nextId++
            entities += entity.copy(id = id)
            return id
        }

        override suspend fun getById(id: Long): MeditationReflectionEntity? =
            entities.find { it.id == id }

        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
        }
    }

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

        override fun observeQuickLogs(limit: Int): Flow<List<MoodEntryEntity>> =
            flow.map { list ->
                list.filter {
                    it.source == MoodSource.HOME_SCREEN.dbValue ||
                        it.source == MoodSource.WIDGET.dbValue
                }.take(limit)
            }

        override suspend fun clearAll() {
            entries.clear()
            flow.value = emptyList()
        }
    }

    private class EmptyThoughtDumpDao : com.safehaven.affirmations.data.local.ThoughtDumpDao {
        override suspend fun getAll(): List<ThoughtDumpEntity> = emptyList()
        override fun observeAll() = kotlinx.coroutines.flow.flowOf(emptyList<ThoughtDumpEntity>())
        override fun observeByType(logType: String) = throw UnsupportedOperationException()
        override fun observeLatest() = throw UnsupportedOperationException()
        override suspend fun insert(entity: ThoughtDumpEntity) = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        override suspend fun clearAll() = throw UnsupportedOperationException()
    }

    private class EmptyMeditationReflectionDao : MeditationReflectionDao {
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun getAll(): List<MeditationReflectionEntity> = emptyList()
        override suspend fun insert(entity: MeditationReflectionEntity) = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class EmptyFutureSelfMessageDao : com.safehaven.affirmations.data.local.FutureSelfMessageDao {
        override suspend fun getAll(): List<FutureSelfMessageEntity> = emptyList()
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun getPendingAfter(nowMillis: Long) = throw UnsupportedOperationException()
        override suspend fun getOverdueUndelivered(nowMillis: Long) = throw UnsupportedOperationException()
        override suspend fun insert(entity: FutureSelfMessageEntity) = throw UnsupportedOperationException()
        override suspend fun update(entity: FutureSelfMessageEntity) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        override suspend fun markDelivered(id: Long) = throw UnsupportedOperationException()
    }

    private class EmptyRefactoringEntryDao : com.safehaven.affirmations.data.local.RefactoringEntryDao {
        override suspend fun getAll(): List<RefactoringEntryEntity> = emptyList()
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun insert(entity: RefactoringEntryEntity) = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class EmptyCenterOfGravityEntryDao :
        com.safehaven.affirmations.data.local.CenterOfGravityEntryDao {
        override suspend fun getAll(): List<CenterOfGravityEntryEntity> = emptyList()
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun insert(entity: CenterOfGravityEntryEntity) = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }

    private class EmptyNvcEntryDao : com.safehaven.affirmations.data.local.NvcEntryDao {
        override suspend fun getAll(): List<NvcEntryEntity> = emptyList()
        override fun observeAll() = throw UnsupportedOperationException()
        override suspend fun insert(entity: NvcEntryEntity) = throw UnsupportedOperationException()
        override suspend fun getById(id: Long) = throw UnsupportedOperationException()
        override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
    }
}
