package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.AffirmationDao
import com.example.meditationparticles.data.local.AffirmationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

        val ordered = dao.entries.filter { !it.archived }.sortedBy { it.sortOrder }
        assertEquals(listOf("B", "C", "A"), ordered.map { it.text })
        assertEquals(listOf(0, 1, 2), ordered.map { it.sortOrder })
    }

    @Test
    fun archive_hidesFromActiveList() = runBlocking {
        val dao = FakeAffirmationDao(
            listOf(
                AffirmationEntity(id = 1, text = "Keep", sortOrder = 0),
                AffirmationEntity(id = 2, text = "Archive me", sortOrder = 1),
            ),
        )
        val repository = AffirmationRepository(dao)

        repository.archive(dao.entries.first { it.id == 2L })

        assertEquals(listOf("Keep"), dao.getAll("Affirmations").map { it.text })
        assertEquals(listOf("Archive me"), dao.observeArchived("Affirmations").first().map { it.text })
        assertTrue(dao.entries.first { it.id == 2L }.archived)
    }

    @Test
    fun unarchive_restoresToEndOfActiveList() = runBlocking {
        val dao = FakeAffirmationDao(
            listOf(
                AffirmationEntity(id = 1, text = "Active", sortOrder = 0),
                AffirmationEntity(id = 2, text = "Archived", sortOrder = 5, archived = true),
            ),
        )
        val repository = AffirmationRepository(dao)

        repository.unarchive(dao.entries.first { it.id == 2L })

        val active = dao.getAll("Affirmations")
        assertEquals(listOf("Active", "Archived"), active.map { it.text })
        assertEquals(1, active.first { it.id == 2L }.sortOrder)
        assertFalse(active.first { it.id == 2L }.archived)
    }

    @Test
    fun reorder_ignoresArchivedItems() = runBlocking {
        val dao = FakeAffirmationDao(
            listOf(
                AffirmationEntity(id = 1, text = "A", sortOrder = 0),
                AffirmationEntity(id = 2, text = "B", sortOrder = 1),
                AffirmationEntity(id = 3, text = "Archived", sortOrder = 99, archived = true),
            ),
        )
        val repository = AffirmationRepository(dao)

        repository.reorder(fromIndex = 0, toIndex = 1)

        val active = dao.entries.filter { !it.archived }.sortedBy { it.sortOrder }
        assertEquals(listOf("B", "A"), active.map { it.text })
        assertTrue(dao.entries.first { it.id == 3L }.archived)
        assertEquals(99, dao.entries.first { it.id == 3L }.sortOrder)
    }

    private class FakeAffirmationDao(
        initial: List<AffirmationEntity>,
    ) : AffirmationDao {
        val entries = initial.toMutableList()
        private val flow = MutableStateFlow(entries.toList())

        override fun observeAll(listKind: String): Flow<List<AffirmationEntity>> =
            flow.asStateFlow().map { list ->
                list.filter { it.listKind == listKind && !it.archived }
            }

        override fun observeArchived(listKind: String): Flow<List<AffirmationEntity>> =
            flow.asStateFlow().map { list ->
                list.filter { it.listKind == listKind && it.archived }
            }

        override suspend fun getAll(listKind: String): List<AffirmationEntity> =
            entries.filter { it.listKind == listKind && !it.archived }

        override suspend fun getAllKinds(): List<AffirmationEntity> = entries.toList()

        override suspend fun count(listKind: String): Int =
            entries.count { it.listKind == listKind }

        override suspend fun countActive(listKind: String): Int =
            entries.count { it.listKind == listKind && !it.archived }

        override suspend fun random(listKind: String): AffirmationEntity? =
            entries.filter { it.listKind == listKind && !it.archived }.randomOrNull()

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
