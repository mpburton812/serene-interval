package com.example.meditationparticles.domain.quickstart

import com.example.meditationparticles.domain.settings.ExperienceSettings

object QuickStartLayout {
    const val SELECTION_COUNT = 4

    val displayOrder: List<QuickStartId> = QuickStartId.entries.toList()

    fun isToolEnabled(id: QuickStartId, settings: ExperienceSettings): Boolean = when (id) {
        QuickStartId.BREATHING -> settings.enableBreathing
        QuickStartId.TIMER -> settings.enableTimer
        QuickStartId.AFFIRMATIONS -> settings.enableAffirmations
        QuickStartId.TOOLKIT -> settings.enableToolkit
        QuickStartId.VISUALS -> settings.enableVisuals
    }

    fun availableIds(settings: ExperienceSettings): List<QuickStartId> =
        displayOrder.filter { isToolEnabled(it, settings) }

    fun defaultSelection(settings: ExperienceSettings): List<QuickStartId> =
        availableIds(settings).take(SELECTION_COUNT)

    fun normalizeSelection(
        selected: List<QuickStartId>,
        settings: ExperienceSettings,
    ): List<QuickStartId> {
        val available = availableIds(settings)
        val availableSet = available.toSet()
        val filtered = selected.filter { it in availableSet }.distinct()
        if (filtered.size >= SELECTION_COUNT) {
            return filtered.take(SELECTION_COUNT)
        }
        val filled = (filtered + available).distinct().take(SELECTION_COUNT)
        return filled
    }

    fun toggleSelection(
        current: List<QuickStartId>,
        id: QuickStartId,
        settings: ExperienceSettings,
    ): List<QuickStartId> {
        if (!isToolEnabled(id, settings)) return current
        if (id in current) return current.filter { it != id }
        if (current.size >= SELECTION_COUNT) return current
        return current + id
    }

    fun hasValidSelection(selection: List<QuickStartId>, settings: ExperienceSettings): Boolean {
        if (selection.size != SELECTION_COUNT) return false
        val available = availableIds(settings).toSet()
        return selection.all { it in available }
    }
}
