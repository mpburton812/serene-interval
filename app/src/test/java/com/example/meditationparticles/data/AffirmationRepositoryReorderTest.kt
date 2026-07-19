package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.AffirmationDao
import com.example.meditationparticles.data.local.AffirmationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AffirmationRepositoryReorderTest {
    @Test
    fun reorder_persistsUpdatedSortOrders() = runBlocking {
        val dao = FakeAffirmationDao(
            listOf(
                AffirmationEntity(id = 1, text = "A", sortOrder = 0),
                AffirmationEntity(id = 2, text = "B", sortOrder = 1),
                AffirmationEntity(id = 3, text = "C", sortOrder = 2),
            ),
        )
        val repository = AffirmationRepository(dao)

        repository.reorder(fromIndex = 0, toIndex = 2)

        val ordered = dao.activeEntries.sortedBy { it.sortOrder }
        assertEquals(listOf("B", "C", "A"), ordered.map { it.text })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun archive_hidesFromActiveList() = runBlocking {
        val entity = AffirmationEntity(id = 1, text = "A", sortOrder = 0)
        val dao = FakeAffirmationDao(listOf(entity))
        val repository = AffirmationRepository(dao)

        repository.archive(entity)

        assertTrue(dao.activeEntries.isEmpty())
        assertEquals(1, dao.archivedEntries.size)
        assertTrue(dao.archivedEntries.single().isArchived)
    }

    @Test
    fun unarchive_restoresToActiveListAtEnd() = runBlocking {
        val active = AffirmationEntity(id = 1, text = "A", sortOrder = 0)
        val archived = AffirmationEntity(id = 2, text = "B", sortOrder = 1, isArchived = true)
        val dao = FakeAffirmationDao(listOf(active, archived))
        val repository = AffirmationRepository(dao)

        repository.unarchive(archived)

        assertEquals(listOf("A", "B"), dao.activeEntries.sortedBy { it.sortOrder }.map { it.text })
        assertEquals(1, dao.activeEntries.last().sortOrder)
        assertTrue(dao.archivedEntries.isEmpty())
    }

    private class FakeAffirmationDao(
        initial: List<AffirmationEntity>,
    ) : AffirmationDao {
        val entries = initial.toMutableList()
        private val flow = MutableStateFlow(entries.toList())

        val activeEntries: List<AffirmationEntity>
            get() = entries.filter { !it.isArchived }

        val archivedEntries: List<AffirmationEntity>
            get() = entries.filter { it.isArchived }

        override fun observeActive(listKind: String): Flow<List<AffirmationEntity>> =
            flow.asStateFlow().map { list ->
                list.filter { it.listKind == listKind && !it.isArchived }
            }

        override fun observeArchived(listKind: String): Flow<List<AffirmationEntity>> =
            flow.asStateFlow().map { list ->
                list.filter { it.listKind == listKind && it.isArchived }
            }

        override suspend fun getActive(listKind: String): List<AffirmationEntity> =
            entries.filter { it.listKind == listKind && !it.isArchived }

        override suspend fun getAllKinds(): List<AffirmationEntity> = entries.toList()

        override suspend fun countAll(listKind: String): Int =
            entries.count { it.listKind == listKind }

        override suspend fun countActive(listKind: String): Int =
            entries.count { it.listKind == listKind && !it.isArchived }

        override suspend fun randomActive(listKind: String): AffirmationEntity? =
            entries.filter { it.listKind == listKind && !it.isArchived }.randomOrNull()

        override suspend fun insert(entity: AffirmationEntity): Long {
            val id = (entries.maxOfOrNull { it.id } ?: 0L) + 1
            entries += entity.copy(id = id)
            flow.value = entries.toList()
            return id
        }

        override suspend fun insertAll(entities: List<AffirmationEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun update(entity: AffirmationEntity) {
            val index = entries.indexOfFirst { it.id == entity.id }
            if (index >= 0) {
                entries[index] = entity
                flow.value = entries.toList()
            }
        }

        override suspend fun updateAll(entities: List<AffirmationEntity>) {
            entities.forEach { update(it) }
        }

        override suspend fun delete(entity: AffirmationEntity) {
            entries.removeAll { it.id == entity.id }
            flow.value = entries.toList()
        }
    }
}
