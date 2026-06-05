package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.LivingTreePersonDao
import com.example.meditationparticles.data.local.LivingTreePersonEntity
import com.example.meditationparticles.data.local.LivingTreePersonTagCrossRef
import com.example.meditationparticles.data.local.LivingTreePersonTagDao
import com.example.meditationparticles.data.local.LivingTreePersonWithTags
import com.example.meditationparticles.data.local.LivingTreeTagDao
import com.example.meditationparticles.data.local.LivingTreeTagEntity
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeRepositoryTest {
    @Test
    fun seedDefaultTagsIfEmpty_insertsAllDefaultTags() = runTest {
        val tagDao = FakeLivingTreeTagDao()
        val repository = createRepository(tagDao)

        repository.seedDefaultTagsIfEmpty()

        assertEquals(LivingTreeDefaults.defaultTags.size, tagDao.tags.size)
        assertTrue(tagDao.tags.any { it.name == "Chosen family" })
        assertTrue(tagDao.tags.any { it.name == "Trans family" })
    }

    @Test
    fun seedDefaultTagsIfEmpty_doesNotDuplicate() = runTest {
        val tagDao = FakeLivingTreeTagDao(
            initial = listOf(LivingTreeTagEntity(id = 1L, name = "Family", colorArgb = 1, sortOrder = 0)),
        )
        val repository = createRepository(tagDao)

        repository.seedDefaultTagsIfEmpty()

        assertEquals(1, tagDao.tags.size)
    }

    @Test
    fun createPerson_rejectsDuplicateNamesCaseInsensitive() = runTest {
        val repository = createRepository()
        repository.createPerson("Alex")

        try {
            repository.createPerson("alex")
            error("Expected duplicate name exception")
        } catch (error: DuplicateLivingTreeNameException) {
            assertEquals(DuplicateLivingTreeNameException.Kind.Person, error.kind)
        }
    }

    @Test
    fun createTag_rejectsDuplicateNamesCaseInsensitive() = runTest {
        val repository = createRepository()
        repository.createTag("Support", colorArgb = 0xFF000000.toInt())

        try {
            repository.createTag("support", colorArgb = 0xFF111111.toInt())
            error("Expected duplicate name exception")
        } catch (error: DuplicateLivingTreeNameException) {
            assertEquals(DuplicateLivingTreeNameException.Kind.Tag, error.kind)
        }
    }

    @Test
    fun countPeopleForTag_returnsDistinctPeople() = runTest {
        val personTagDao = FakeLivingTreePersonTagDao()
        val tagDao = FakeLivingTreeTagDao(personTagDao = personTagDao)
        val personDao = FakeLivingTreePersonDao()
        val repository = LivingTreeRepository(tagDao, personDao, personTagDao)

        val tagId = tagDao.insert(LivingTreeTagEntity(name = "Friends", colorArgb = 1))
        val personId = personDao.insert(LivingTreePersonEntity(name = "Sam"))
        personTagDao.insertAll(listOf(LivingTreePersonTagCrossRef(personId, tagId)))

        assertEquals(1, repository.countPeopleForTag(tagId))
    }

    @Test
    fun setPersonTags_replacesExistingAssignments() = runTest {
        val tagDao = FakeLivingTreeTagDao()
        val personDao = FakeLivingTreePersonDao()
        val personTagDao = FakeLivingTreePersonTagDao()
        val repository = LivingTreeRepository(tagDao, personDao, personTagDao)

        val supportId = tagDao.insert(LivingTreeTagEntity(name = "Support", colorArgb = 1))
        val familyId = tagDao.insert(LivingTreeTagEntity(name = "Family", colorArgb = 2))
        val personId = personDao.insert(LivingTreePersonEntity(name = "Jordan"))
        repository.setPersonTags(personId, setOf(supportId))
        repository.setPersonTags(personId, setOf(familyId))

        assertEquals(listOf(familyId), personTagDao.getTagIdsForPerson(personId))
    }

    private fun createRepository(
        tagDao: FakeLivingTreeTagDao = FakeLivingTreeTagDao(),
    ): LivingTreeRepository =
        LivingTreeRepository(
            tagDao = tagDao,
            personDao = FakeLivingTreePersonDao(),
            personTagDao = FakeLivingTreePersonTagDao(),
        )
}

private class FakeLivingTreeTagDao(
    initial: List<LivingTreeTagEntity> = emptyList(),
    private val personTagDao: FakeLivingTreePersonTagDao? = null,
) : LivingTreeTagDao {
    private val state = MutableStateFlow(initial.toMutableList())
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L
    val tags: List<LivingTreeTagEntity> get() = state.value

    override fun observeAll(): Flow<List<LivingTreeTagEntity>> = state

    override suspend fun getAll(): List<LivingTreeTagEntity> = state.value

    override suspend fun count(): Int = state.value.size

    override suspend fun getById(id: Long): LivingTreeTagEntity? =
        state.value.firstOrNull { it.id == id }

    override suspend fun findByNameIgnoreCase(name: String): LivingTreeTagEntity? =
        state.value.firstOrNull { it.name.equals(name, ignoreCase = true) }

    override suspend fun countPeopleForTag(tagId: Long): Int =
        personTagDao?.refs.orEmpty().filter { it.tagId == tagId }.map { it.personId }.distinct().size

    override suspend fun insert(entity: LivingTreeTagEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        state.value = (state.value + entity.copy(id = id)).toMutableList()
        return id
    }

    override suspend fun update(entity: LivingTreeTagEntity) {
        state.value = state.value.map { if (it.id == entity.id) entity else it }.toMutableList()
    }

    override suspend fun delete(entity: LivingTreeTagEntity) {
        state.value = state.value.filterNot { it.id == entity.id }.toMutableList()
    }
}

private class FakeLivingTreePersonDao(
    initial: List<LivingTreePersonEntity> = emptyList(),
) : LivingTreePersonDao {
    private val people = MutableStateFlow(initial.toMutableList())
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observePeopleWithTags(): Flow<List<LivingTreePersonWithTags>> =
        people.map { list -> list.map { LivingTreePersonWithTags(it, emptyList()) } }

    override suspend fun getPeopleWithTags(): List<LivingTreePersonWithTags> =
        people.value.map { LivingTreePersonWithTags(it, emptyList()) }

    override suspend fun count(): Int = people.value.size

    override suspend fun getById(id: Long): LivingTreePersonEntity? =
        people.value.firstOrNull { it.id == id }

    override suspend fun findByNameIgnoreCase(name: String): LivingTreePersonEntity? =
        people.value.firstOrNull { it.name.equals(name, ignoreCase = true) }

    override suspend fun insert(entity: LivingTreePersonEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        people.value = (people.value + entity.copy(id = id)).toMutableList()
        return id
    }

    override suspend fun update(entity: LivingTreePersonEntity) {
        people.value = people.value.map { if (it.id == entity.id) entity else it }.toMutableList()
    }

    override suspend fun delete(entity: LivingTreePersonEntity) {
        people.value = people.value.filterNot { it.id == entity.id }.toMutableList()
    }
}

private class FakeLivingTreePersonTagDao : LivingTreePersonTagDao {
    val refs = mutableListOf<LivingTreePersonTagCrossRef>()

    override suspend fun getTagIdsForPerson(personId: Long): List<Long> =
        refs.filter { it.personId == personId }.map { it.tagId }

    override suspend fun insertAll(items: List<LivingTreePersonTagCrossRef>) {
        refs += items
    }

    override suspend fun deleteForPerson(personId: Long) {
        refs.removeAll { it.personId == personId }
    }
}
