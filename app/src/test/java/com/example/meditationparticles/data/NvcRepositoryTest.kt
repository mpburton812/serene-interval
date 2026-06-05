package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.NvcEntryDao
import com.example.meditationparticles.data.local.NvcEntryEntity
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
        val repository = NvcRepository(dao)
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
        val repository = NvcRepository(dao)
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
    fun deleteEntry_removesRowAndClearsAudioPaths() = runTest {
        val dao = FakeNvcEntryDao()
        val repository = NvcRepository(dao)
        val id = repository.save(
            NvcEntryEntity(
                observation = "Test",
                feeling = "",
                need = "",
                request = "",
                observationAudioPath = "/tmp/nvc-audio.m4a",
            ),
        )!!
        repository.deleteEntry(id)
        assertEquals(0, dao.entries.size)
    }

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
