package com.example.meditationparticles.domain.quickstart

import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId

object QuickStartLayout {
    const val SELECTION_COUNT = 4

    fun displayOrder(
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> {
        val breathing = if (settings.enableBreathing) {
            BreathingPattern.All.map { QuickStartTarget.Breathing(it.id) }
        } else {
            emptyList()
        }
        val core = buildList {
            if (settings.enableTimer) add(QuickStartTarget.Timer)
            if (settings.enableAffirmations) add(QuickStartTarget.Affirmations)
            if (settings.enableVisuals) add(QuickStartTarget.Visuals)
        }
        val toolkit = if (settings.enableToolkit) {
            ToolkitCatalog.all
                .filter { it.id in enabledToolkitTools }
                .map { QuickStartTarget.Toolkit(it.id) }
        } else {
            emptyList()
        }
        return breathing + core + toolkit
    }

    fun isTargetEnabled(
        target: QuickStartTarget,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): Boolean = when (target) {
        is QuickStartTarget.Breathing -> settings.enableBreathing &&
            BreathingPattern.All.any { it.id == target.patternId }
        QuickStartTarget.Timer -> settings.enableTimer
        QuickStartTarget.Affirmations -> settings.enableAffirmations
        QuickStartTarget.Visuals -> settings.enableVisuals
        is QuickStartTarget.Toolkit -> settings.enableToolkit && target.toolId in enabledToolkitTools
    }

    fun availableTargets(
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> =
        displayOrder(settings, enabledToolkitTools)
            .filter { isTargetEnabled(it, settings, enabledToolkitTools) }

    fun defaultSelection(
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> {
        val preferred = buildList {
            if (settings.enableBreathing) {
                add(QuickStartTarget.Breathing(BreathingPattern.BoxBreathing.id))
            }
            if (settings.enableTimer) add(QuickStartTarget.Timer)
            if (settings.enableAffirmations) add(QuickStartTarget.Affirmations)
            if (settings.enableToolkit) {
                val firstTool = ToolkitCatalog.all.firstOrNull { it.id in enabledToolkitTools }
                if (firstTool != null) add(QuickStartTarget.Toolkit(firstTool.id))
            }
        }
        return normalizeSelection(preferred, settings, enabledToolkitTools)
    }

    /** Drops invalid targets and caps at [SELECTION_COUNT]; does not pad partial selections. */
    fun sanitizeSelection(
        selected: List<QuickStartTarget>,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> {
        val availableSet = availableTargets(settings, enabledToolkitTools).toSet()
        return selected.filter { it in availableSet }.distinct().take(SELECTION_COUNT)
    }

    /** Sanitizes then fills up to [SELECTION_COUNT] from available targets (defaults, import, prune). */
    fun normalizeSelection(
        selected: List<QuickStartTarget>,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> {
        val sanitized = sanitizeSelection(selected, settings, enabledToolkitTools)
        if (sanitized.size >= SELECTION_COUNT) return sanitized
        val available = availableTargets(settings, enabledToolkitTools)
        return (sanitized + available).distinct().take(SELECTION_COUNT)
    }

    fun toggleSelection(
        current: List<QuickStartTarget>,
        target: QuickStartTarget,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): List<QuickStartTarget> {
        if (!isTargetEnabled(target, settings, enabledToolkitTools)) return current
        if (target in current) return current.filter { it != target }
        if (current.size >= SELECTION_COUNT) return current
        return current + target
    }

    fun hasValidSelection(
        selection: List<QuickStartTarget>,
        settings: ExperienceSettings,
        enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    ): Boolean {
        if (selection.size != SELECTION_COUNT) return false
        val available = availableTargets(settings, enabledToolkitTools).toSet()
        return selection.all { it in available }
    }
}
