package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.AffirmationReviewSessionDao
import com.safehaven.affirmations.data.local.AffirmationReviewSessionEntity
import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
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
import org.junit.Assert.assertTrue
import org.junit.Test

class AffirmationReviewSessionRepositoryTest {
    @Test
    fun save_persistsSessionAndRecordsMood() = runTest {
        val dao = FakeAffirmationReviewSessionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = AffirmationReviewSessionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        val id = repository.save(
            notes = "  Felt steady  ",
            affirmationCount = 5,
            completedAt = 9_000L,
            moodLevel = 4,
        )

        assertEquals(1L, id)
        val saved = dao.entities.single()
        assertEquals("Felt steady", saved.notes)
        assertEquals(5, saved.affirmationCount)
        assertEquals(9_000L, saved.completedAt)
        assertEquals(4, saved.moodLevel)

        val moodEntry = moodDao.entries.single()
        assertEquals(MoodSource.AFFIRMATION_REVIEW.dbValue, moodEntry.source)
        assertEquals(4, moodEntry.moodLevel)
        assertEquals(MoodTrackerRepository.TABLE_AFFIRMATION_REVIEW_SESSIONS, moodEntry.legacyTable)
        assertEquals(1L, moodEntry.legacyRowId)
    }

    @Test
    fun save_withoutMood_skipsMoodTracker() = runTest {
        val dao = FakeAffirmationReviewSessionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = AffirmationReviewSessionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        repository.save(
            notes = "Quick note",
            affirmationCount = 2,
            completedAt = 1_000L,
            moodLevel = null,
        )

        assertTrue(moodDao.entries.isEmpty())
        assertEquals("Quick note", dao.entities.single().notes)
    }

    @Test
    fun save_normalizesLegacyMoodLevel() = runTest {
        val dao = FakeAffirmationReviewSessionDao()
        val moodDao = FakeMoodEntryDao()
        val repository = AffirmationReviewSessionRepository(
            dao = dao,
            moodTracker = moodTracker(moodDao),
        )

        repository.save(
            notes = "",
            affirmationCount = 1,
            completedAt = 1_000L,
            moodLevel = 5,
        )

        assertEquals(4, dao.entities.single().moodLevel)
        assertEquals(4, moodDao.entries.single().moodLevel)
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

    private class FakeAffirmationReviewSessionDao : AffirmationReviewSessionDao {
        val entities = mutableListOf<AffirmationReviewSessionEntity>()
        private var nextId = 1L

        override suspend fun getAll(listKind: String): List<AffirmationReviewSessionEntity> =
            entities.filter { it.listKind == listKind }

        override suspend fun getAllKinds(): List<AffirmationReviewSessionEntity> = entities.toList()

        override fun observeAll(listKind: String) =
            kotlinx.coroutines.flow.flowOf(entities.filter { it.listKind == listKind })

        override fun observeAllKinds() =
            kotlinx.coroutines.flow.flowOf(entities.toList())

        override fun observeInRange(listKind: String, startMillis: Long, endMillis: Long) =
            kotlinx.coroutines.flow.flowOf(
                entities.filter {
                    it.listKind == listKind && it.completedAt in startMillis until endMillis
                },
            )

        override suspend fun insert(entity: AffirmationReviewSessionEntity): Long {
            val id = nextId++
            entities += entity.copy(id = id)
            return id
        }

        override suspend fun getById(id: Long): AffirmationReviewSessionEntity? =
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

    private class EmptyMeditationReflectionDao : com.safehaven.affirmations.data.local.MeditationReflectionDao {
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
