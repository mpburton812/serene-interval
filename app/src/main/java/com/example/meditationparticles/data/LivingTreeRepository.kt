package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.LivingTreePersonDao
import com.example.meditationparticles.data.local.LivingTreePersonEntity
import com.example.meditationparticles.data.local.LivingTreePersonTagCrossRef
import com.example.meditationparticles.data.local.LivingTreePersonTagDao
import com.example.meditationparticles.data.local.LivingTreePersonWithTags
import com.example.meditationparticles.data.local.LivingTreeTagDao
import com.example.meditationparticles.data.local.LivingTreeTagEntity
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import com.example.meditationparticles.domain.livingtree.LivingTreeLayout
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DuplicateLivingTreeNameException(
    val kind: Kind,
) : Exception() {
    enum class Kind { Person, Tag }
}

data class BulkCreatePeopleResult(
    val createdCount: Int,
    val skippedDuplicates: List<String>,
)

class LivingTreeRepository(
    private val tagDao: LivingTreeTagDao,
    private val personDao: LivingTreePersonDao,
    private val personTagDao: LivingTreePersonTagDao,
) {
    val tags: Flow<List<LivingTreeTagEntity>> = tagDao.observeAll()

    val peopleWithTags: Flow<List<LivingTreePersonWithTags>> = personDao.observePeopleWithTags()

    val snapshot: Flow<LivingTreeSnapshot> = combine(peopleWithTags, tags) { people, tagList ->
        LivingTreeSnapshot(
            people = people,
            tagById = tagList.associateBy { it.id },
        )
    }

    suspend fun seedDefaultTagsIfEmpty() {
        if (tagDao.count() > 0) return
        val now = System.currentTimeMillis()
        LivingTreeDefaults.defaultTags.forEachIndexed { index, default ->
            tagDao.insert(
                LivingTreeTagEntity(
                    name = default.name,
                    colorArgb = default.colorArgb,
                    sortOrder = index,
                    createdAtMillis = now - index,
                ),
            )
        }
    }

    suspend fun getAllTags(): List<LivingTreeTagEntity> = tagDao.getAll()

    suspend fun getAllPeopleWithTags(): List<LivingTreePersonWithTags> =
        personDao.getPeopleWithTags()

    suspend fun createTag(name: String, colorArgb: Int): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Tag name cannot be empty." }
        if (tagDao.findByNameIgnoreCase(trimmed) != null) {
            throw DuplicateLivingTreeNameException(DuplicateLivingTreeNameException.Kind.Tag)
        }
        val sortOrder = tagDao.count()
        return tagDao.insert(
            LivingTreeTagEntity(
                name = trimmed,
                colorArgb = colorArgb,
                sortOrder = sortOrder,
            ),
        )
    }

    suspend fun updateTag(entity: LivingTreeTagEntity) {
        val trimmed = entity.name.trim()
        require(trimmed.isNotEmpty()) { "Tag name cannot be empty." }
        val existing = tagDao.findByNameIgnoreCase(trimmed)
        if (existing != null && existing.id != entity.id) {
            throw DuplicateLivingTreeNameException(DuplicateLivingTreeNameException.Kind.Tag)
        }
        tagDao.update(entity.copy(name = trimmed))
    }

    suspend fun deleteTag(entity: LivingTreeTagEntity) {
        tagDao.delete(entity)
    }

    suspend fun createPerson(
        name: String,
        notes: String = "",
        tagIds: Set<Long> = emptySet(),
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Person name cannot be empty." }
        if (personDao.findByNameIgnoreCase(trimmed) != null) {
            throw DuplicateLivingTreeNameException(DuplicateLivingTreeNameException.Kind.Person)
        }
        val count = personDao.count()
        val now = System.currentTimeMillis()
        val personId = personDao.insert(
            LivingTreePersonEntity(
                name = trimmed,
                notes = notes.trim(),
                sortOrder = count,
                angleRadians = LivingTreeLayout.angleForNewPerson(count),
                createdAtMillis = now,
                updatedAtMillis = now,
            ),
        )
        setPersonTags(personId, tagIds)
        return personId
    }

    suspend fun createPeople(
        names: List<String>,
        notes: String = "",
        tagIds: Set<Long> = emptySet(),
    ): BulkCreatePeopleResult {
        require(names.isNotEmpty()) { "At least one person name is required." }
        val applyNotes = names.size == 1
        val trimmedNotes = if (applyNotes) notes.trim() else ""
        val seen = mutableSetOf<String>()
        val skipped = mutableListOf<String>()
        var created = 0
        for (name in names) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) continue
            val key = trimmed.lowercase()
            if (!seen.add(key)) {
                skipped.add(trimmed)
                continue
            }
            if (personDao.findByNameIgnoreCase(trimmed) != null) {
                skipped.add(trimmed)
                continue
            }
            val count = personDao.count()
            val now = System.currentTimeMillis()
            val personId = personDao.insert(
                LivingTreePersonEntity(
                    name = trimmed,
                    notes = trimmedNotes,
                    sortOrder = count,
                    angleRadians = LivingTreeLayout.angleForNewPerson(count),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )
            setPersonTags(personId, tagIds)
            created++
        }
        return BulkCreatePeopleResult(createdCount = created, skippedDuplicates = skipped)
    }

    suspend fun updatePerson(
        entity: LivingTreePersonEntity,
        tagIds: Set<Long>,
    ) {
        val trimmed = entity.name.trim()
        require(trimmed.isNotEmpty()) { "Person name cannot be empty." }
        val existing = personDao.findByNameIgnoreCase(trimmed)
        if (existing != null && existing.id != entity.id) {
            throw DuplicateLivingTreeNameException(DuplicateLivingTreeNameException.Kind.Person)
        }
        personDao.update(
            entity.copy(
                name = trimmed,
                notes = entity.notes.trim(),
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
        setPersonTags(entity.id, tagIds)
    }

    suspend fun deletePerson(entity: LivingTreePersonEntity) {
        personTagDao.deleteForPerson(entity.id)
        personDao.delete(entity)
    }

    suspend fun updatePersonAngle(personId: Long, angleRadians: Double) {
        val person = personDao.getById(personId) ?: return
        personDao.update(
            person.copy(
                angleRadians = angleRadians,
                updatedAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setPersonTags(personId: Long, tagIds: Set<Long>) {
        personTagDao.deleteForPerson(personId)
        if (tagIds.isNotEmpty()) {
            personTagDao.insertAll(
                tagIds.map { tagId ->
                    LivingTreePersonTagCrossRef(personId = personId, tagId = tagId)
                },
            )
        }
    }

    suspend fun countPeopleForTag(tagId: Long): Int = tagDao.countPeopleForTag(tagId)

    suspend fun replaceAllFromExport(
        tags: List<LivingTreeTagEntity>,
        people: List<LivingTreePersonEntity>,
        personTags: List<LivingTreePersonTagCrossRef>,
    ) {
        val existingPeople = personDao.getPeopleWithTags()
        existingPeople.forEach { personTagDao.deleteForPerson(it.person.id) }
        existingPeople.forEach { personDao.delete(it.person) }
        tagDao.getAll().forEach { tagDao.delete(it) }

        val tagIdMap = mutableMapOf<Long, Long>()
        tags.sortedBy { it.sortOrder }.forEach { tag ->
            val newId = tagDao.insert(
                tag.copy(id = 0L),
            )
            tagIdMap[tag.id] = newId
        }

        val personIdMap = mutableMapOf<Long, Long>()
        people.sortedBy { it.sortOrder }.forEach { person ->
            val newId = personDao.insert(person.copy(id = 0L))
            personIdMap[person.id] = newId
        }

        val refs = personTags.mapNotNull { ref ->
            val personId = personIdMap[ref.personId] ?: return@mapNotNull null
            val tagId = tagIdMap[ref.tagId] ?: return@mapNotNull null
            LivingTreePersonTagCrossRef(personId = personId, tagId = tagId)
        }
        if (refs.isNotEmpty()) {
            personTagDao.insertAll(refs)
        }
    }
}

data class LivingTreeSnapshot(
    val people: List<LivingTreePersonWithTags>,
    val tagById: Map<Long, LivingTreeTagEntity>,
)
