package com.safehaven.affirmations.ui.livingtree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.data.local.LivingTreePersonWithTags
import com.safehaven.affirmations.data.local.LivingTreeTagEntity
import com.safehaven.affirmations.domain.livingtree.LivingTreeDefaults
import com.safehaven.affirmations.domain.livingtree.LivingTreeFilterLogic
import com.safehaven.affirmations.domain.settings.ExperienceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LivingTreeUiState(
    val centerLabel: String = "You",
    val tags: List<LivingTreeTagEntity> = emptyList(),
    val tagCounts: Map<Long, Int> = emptyMap(),
    val selectedTagIds: List<Long> = emptyList(),
    val allPeople: List<LivingTreePersonWithTags> = emptyList(),
    val visiblePeople: List<LivingTreePersonWithTags> = emptyList(),
    val selectedPerson: LivingTreePersonWithTags? = null,
    val editingPerson: LivingTreePersonWithTags? = null,
    val filterActive: Boolean = false,
)

class LivingTreeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppGraph.livingTree(application)
    private val settingsFlow = AppGraph.settings(application).settings

    private val selectedTagIds = MutableStateFlow<List<Long>>(emptyList())
    private val selectedPersonId = MutableStateFlow<Long?>(null)
    private val editingPersonId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LivingTreeUiState> = combine(
        repository.snapshot,
        settingsFlow,
        selectedTagIds,
        selectedPersonId,
        editingPersonId,
    ) { snapshot, settings, selected, personId, editingId ->
        buildUiState(snapshot.people, snapshot.tagById, settings, selected, personId, editingId)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LivingTreeUiState(),
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultTagsIfEmpty()
        }
    }

    fun toggleTagFilter(tagId: Long) {
        selectedTagIds.value = selectedTagIds.value.toMutableList().apply {
            if (contains(tagId)) {
                remove(tagId)
            } else if (size < LivingTreeDefaults.MAX_FILTER_TAGS) {
                add(tagId)
            }
        }
    }

    fun clearTagFilter() {
        selectedTagIds.value = emptyList()
    }

    fun selectPerson(person: LivingTreePersonWithTags) {
        selectedPersonId.value = person.person.id
    }

    fun dismissPersonDetail() {
        selectedPersonId.value = null
    }

    fun startEditPerson(person: LivingTreePersonWithTags) {
        editingPersonId.value = person.person.id
    }

    fun dismissPersonEditor() {
        editingPersonId.value = null
    }

    fun updatePerson(name: String, notes: String, tagIds: Set<Long>, personId: Long) {
        viewModelScope.launch {
            val current = uiState.value.allPeople.find { it.person.id == personId } ?: return@launch
            runCatching {
                repository.updatePerson(
                    current.person.copy(name = name, notes = notes),
                    tagIds,
                )
            }.onSuccess {
                editingPersonId.value = null
            }
        }
    }

    private fun buildUiState(
        people: List<LivingTreePersonWithTags>,
        tagById: Map<Long, LivingTreeTagEntity>,
        settings: ExperienceSettings,
        selected: List<Long>,
        personId: Long?,
        editingId: Long?,
    ): LivingTreeUiState {
        val peopleTagIds = people.associate { entry ->
            entry.person.id to entry.tags.map { it.id }.toSet()
        }
        val visibleIds = LivingTreeFilterLogic.visiblePersonIds(peopleTagIds, selected.toSet())
        val visiblePeople = if (selected.isEmpty()) people else people.filter { it.person.id in visibleIds }
        val selectedPerson = personId?.let { id -> people.find { it.person.id == id } }
        val editingPerson = editingId?.let { id -> people.find { it.person.id == id } }

        return LivingTreeUiState(
            centerLabel = LivingTreeDefaults.centerLabel(settings.preferredName),
            tags = tagById.values.sortedBy { it.sortOrder },
            tagCounts = LivingTreeFilterLogic.tagPersonCounts(peopleTagIds, tagById.keys.toList()),
            selectedTagIds = selected,
            allPeople = people,
            visiblePeople = visiblePeople,
            selectedPerson = selectedPerson,
            editingPerson = editingPerson,
            filterActive = selected.isNotEmpty(),
        )
    }
}
