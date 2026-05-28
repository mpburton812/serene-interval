package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QuickStartPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _selectedTargets = MutableStateFlow<List<QuickStartTarget>>(emptyList())
    val selectedTargets: StateFlow<List<QuickStartTarget>> = _selectedTargets.asStateFlow()

    fun load(
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = readEnabledToolkitTools(settings),
    ): List<QuickStartTarget> {
        val stored = prefs.getString(KEY_SELECTED_IDS, null)
        val parsed = QuickStartTarget.parseList(stored)
        val sanitized = QuickStartLayout.sanitizeSelection(parsed, settings, enabledToolkitTools)
        val resolved = if (stored == null) {
            QuickStartLayout.defaultSelection(settings, enabledToolkitTools)
        } else {
            sanitized
        }
        if (resolved != parsed) {
            persist(resolved)
        }
        _selectedTargets.value = resolved
        return resolved
    }

    fun refresh(settings: ExperienceSettings) {
        _selectedTargets.value = load(settings)
    }

    fun saveSelection(
        selection: List<QuickStartTarget>,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = readEnabledToolkitTools(settings),
    ) {
        val sanitized = QuickStartLayout.sanitizeSelection(selection, settings, enabledToolkitTools)
        persist(sanitized)
        _selectedTargets.value = sanitized
    }

    private fun persist(selection: List<QuickStartTarget>) {
        prefs.edit()
            .putString(KEY_SELECTED_IDS, selection.joinToString(",") { it.encode() })
            .apply()
    }

    fun updateSelection(
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = readEnabledToolkitTools(settings),
        transform: (List<QuickStartTarget>) -> List<QuickStartTarget>,
    ) {
        saveSelection(transform(_selectedTargets.value), settings, enabledToolkitTools)
    }

    private fun readEnabledToolkitTools(settings: ExperienceSettings): Set<ToolkitToolId> {
        val snapshot = AppGraph.toolkit(appContext).snapshot.value
        return snapshot.enabledToolIds.ifEmpty { ToolkitLayout.defaultEnabledTools() }
    }

    companion object {
        private const val PREFS_NAME = "quick_start_preferences"
        private const val KEY_SELECTED_IDS = "selected_ids"
    }
}
