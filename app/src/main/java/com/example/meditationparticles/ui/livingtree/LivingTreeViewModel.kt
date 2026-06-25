package com.example.meditationparticles.ui.livingtree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.LivingTreePersonWithTags
import com.example.meditationparticles.data.local.LivingTreeTagEntity
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import com.example.meditationparticles.domain.livingtree.LivingTreeFilterLogic
import com.example.meditationparticles.domain.settings.ExperienceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    val filterActive: Boolean = false,
)

class LivingTreeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppGraph.livingTree(application)
    private val settingsFlow = AppGraph.settings(application).settings

    private val selectedTagIds = MutableStateFlow<List<Long>>(emptyList())
    private val selectedPerson = MutableStateFlow<LivingTreePersonWithTags?>(null)

    val uiState: StateFlow<LivingTreeUiState> = combine(
        repository.snapshot,
        settingsFlow,
        selectedTagIds,
        selectedPerson,
    ) { snapshot, settings, selected, person ->
        buildUiState(snapshot.people, snapshot.tagById, settings, selected, person)
    }
        .catch { emit(LivingTreeUiState()) }
        .stateIn(
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
        selectedPerson.value = person
    }

    fun dismissPersonDetail() {
        selectedPerson.value = null
    }

    private fun buildUiState(
        people: List<LivingTreePersonWithTags>,
        tagById: Map<Long, LivingTreeTagEntity>,
        settings: ExperienceSettings,
        selected: List<Long>,
        person: LivingTreePersonWithTags?,
    ): LivingTreeUiState {
        val peopleTagIds = people.associate { entry ->
            entry.person.id to entry.tags.map { it.id }.toSet()
        }
        val visibleIds = LivingTreeFilterLogic.visiblePersonIds(peopleTagIds, selected.toSet())
        val visiblePeople = if (selected.isEmpty()) people else people.filter { it.person.id in visibleIds }

        return LivingTreeUiState(
            centerLabel = LivingTreeDefaults.centerLabel(settings.preferredName),
            tags = tagById.values.sortedBy { it.sortOrder },
            tagCounts = LivingTreeFilterLogic.tagPersonCounts(peopleTagIds, tagById.keys.toList()),
            selectedTagIds = selected,
            allPeople = people,
            visiblePeople = visiblePeople,
            selectedPerson = person,
            filterActive = selected.isNotEmpty(),
        )
    }
}
