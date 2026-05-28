package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.quickstart.QuickStartId
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.settings.ExperienceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuickStartPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _selectedIds = MutableStateFlow<List<QuickStartId>>(emptyList())
    val selectedIds: StateFlow<List<QuickStartId>> = _selectedIds.asStateFlow()

    fun load(settings: ExperienceSettings): List<QuickStartId> {
        val stored = prefs.getString(KEY_SELECTED_IDS, null)
        val parsed = if (stored.isNullOrBlank()) {
            emptyList()
        } else {
            stored.split(",")
                .mapNotNull { name ->
                    runCatching { QuickStartId.valueOf(name.trim()) }.getOrNull()
                }
        }
        val normalized = QuickStartLayout.normalizeSelection(parsed, settings)
        if (normalized != parsed) {
            persist(normalized)
        }
        _selectedIds.value = normalized
        return normalized
    }

    fun refresh(settings: ExperienceSettings) {
        _selectedIds.value = load(settings)
    }

    fun saveSelection(selection: List<QuickStartId>, settings: ExperienceSettings) {
        val normalized = QuickStartLayout.normalizeSelection(selection, settings)
        persist(normalized)
        _selectedIds.value = normalized
    }

    private fun persist(selection: List<QuickStartId>) {
        prefs.edit()
            .putString(KEY_SELECTED_IDS, selection.joinToString(",") { it.name })
            .apply()
    }

    fun updateSelection(
        settings: ExperienceSettings,
        transform: (List<QuickStartId>) -> List<QuickStartId>,
    ) {
        saveSelection(transform(_selectedIds.value), settings)
    }

    companion object {
        private const val PREFS_NAME = "quick_start_preferences"
        private const val KEY_SELECTED_IDS = "selected_ids"
    }
}
