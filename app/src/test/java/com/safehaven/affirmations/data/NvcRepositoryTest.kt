package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.MoodEntryDao
import com.safehaven.affirmations.data.local.MoodEntryEntity
import com.safehaven.affirmations.data.local.NvcEntryDao
import com.safehaven.affirmations.data.local.NvcEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NvcRepositoryTest {
    @Test
    fun save_returnsNullWhenEntryIsEmpty() = runTest {
        val dao = FakeNvcEntryDao()
        val repository = NvcRepository(dao, noopMoodTracker())
        val id = repository.save(
            NvcEntryEntity(
                observation = "",
                feeling = "",
                need = "",
                request = "",
            ),
        )
        assertNull(id)
        assertEquals(0, dao.entries.size)
    }

    @Test
    fun save_persistsEntryWithAnyFieldFilled() = runTest {
        val dao = FakeNvcEntryDao()
        val repository = NvcRepository(dao, noopMoodTracker())
        val id = repository.save(
            NvcEntryEntity(
                observation = "They arrived ten minutes late.",
                feeling = "",
                need = "",
                request = "",
            ),
        )
        assertEquals(1L, id)
        assertEquals("They arrived ten minutes late.", dao.entries.single().observation)
    }

    @Test
    fun deleteEntry_removesRow() = runTest {
        val dao = FakeNvcEntryDao()
        val repository = NvcRepository(dao, noopMoodTracker())
        val id = repository.save(
            NvcEntryEntity(
                observation = "Test",
                feeling = "",
                need = "",
                request = "",
            ),
        )!!
        repository.deleteEntry(id)
        assertEquals(0, dao.entries.size)
    }

    private fun noopMoodTracker(): MoodTrackerRepository = MoodTrackerRepository(
        moodEntryDao = object : MoodEntryDao {
            override suspend fun insert(entity: MoodEntryEntity) = 0L

            override fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<MoodEntryEntity>> =
                MutableStateFlow(emptyList())

            override fun averageInRange(startMillis: Long, endMillis: Long): Flow<Double?> =
                MutableStateFlow(null)

            override suspend fun countLegacy(legacyTable: String, legacyRowId: Long) = 0

            override suspend fun getAll(): List<MoodEntryEntity> = emptyList()

            override suspend fun clearAll() = Unit
        },
        thoughtDumpDao = object : com.safehaven.affirmations.data.local.ThoughtDumpDao {
            override suspend fun getAll() = emptyList<com.safehaven.affirmations.data.local.ThoughtDumpEntity>()

            override fun observeAll() =
                kotlinx.coroutines.flow.flowOf(emptyList<com.safehaven.affirmations.data.local.ThoughtDumpEntity>())

            override fun observeByType(logType: String) = throw UnsupportedOperationException()

            override fun observeLatest() = throw UnsupportedOperationException()

            override suspend fun insert(entity: com.safehaven.affirmations.data.local.ThoughtDumpEntity) =
                throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()

            override suspend fun clearAll() = throw UnsupportedOperationException()
        },
        meditationReflectionDao = object : com.safehaven.affirmations.data.local.MeditationReflectionDao {
            override fun observeAll() = throw UnsupportedOperationException()

            override suspend fun getAll() = emptyList<com.safehaven.affirmations.data.local.MeditationReflectionEntity>()

            override suspend fun insert(entity: com.safehaven.affirmations.data.local.MeditationReflectionEntity) =
                throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        },
        futureSelfMessageDao = object : com.safehaven.affirmations.data.local.FutureSelfMessageDao {
            override suspend fun getAll() = emptyList<com.safehaven.affirmations.data.local.FutureSelfMessageEntity>()

            override fun observeAll() = throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun getPendingAfter(nowMillis: Long) = throw UnsupportedOperationException()

            override suspend fun getOverdueUndelivered(nowMillis: Long) = throw UnsupportedOperationException()

            override suspend fun insert(entity: com.safehaven.affirmations.data.local.FutureSelfMessageEntity) =
                throw UnsupportedOperationException()

            override suspend fun update(entity: com.safehaven.affirmations.data.local.FutureSelfMessageEntity) =
                throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()

            override suspend fun markDelivered(id: Long) = throw UnsupportedOperationException()
        },
        refactoringEntryDao = object : com.safehaven.affirmations.data.local.RefactoringEntryDao {
            override suspend fun getAll() = emptyList<com.safehaven.affirmations.data.local.RefactoringEntryEntity>()

            override fun observeAll() = throw UnsupportedOperationException()

            override suspend fun insert(entity: com.safehaven.affirmations.data.local.RefactoringEntryEntity) =
                throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        },
        centerOfGravityEntryDao = object : com.safehaven.affirmations.data.local.CenterOfGravityEntryDao {
            override suspend fun getAll() = emptyList<com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity>()

            override fun observeAll() = throw UnsupportedOperationException()

            override suspend fun insert(entity: com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity) =
                throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        },
        nvcEntryDao = object : NvcEntryDao {
            override suspend fun getAll() = emptyList<NvcEntryEntity>()

            override fun observeAll() = throw UnsupportedOperationException()

            override suspend fun insert(entity: NvcEntryEntity) = throw UnsupportedOperationException()

            override suspend fun getById(id: Long) = throw UnsupportedOperationException()

            override suspend fun deleteById(id: Long) = throw UnsupportedOperationException()
        },
    )

    private class FakeNvcEntryDao : NvcEntryDao {
        val entries = mutableListOf<NvcEntryEntity>()
        private val flow = MutableStateFlow<List<NvcEntryEntity>>(emptyList())

        override suspend fun getAll(): List<NvcEntryEntity> = entries.toList()

        override fun observeAll(): Flow<List<NvcEntryEntity>> = flow.asStateFlow()

        override suspend fun insert(entity: NvcEntryEntity): Long {
            val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
            val stored = entity.copy(id = id)
            entries += stored
            flow.value = entries.sortedByDescending { it.createdAt }
            return id
        }

        override suspend fun getById(id: Long): NvcEntryEntity? = entries.find { it.id == id }

        override suspend fun deleteById(id: Long) {
            entries.removeAll { it.id == id }
            flow.value = entries.toList()
        }
    }
}
