package com.example.meditationparticles.domain.quickstart

import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.toolkit.ToolkitToolId

sealed class QuickStartTarget {
    data object Timer : QuickStartTarget()
    data object Affirmations : QuickStartTarget()
    data object Visuals : QuickStartTarget()
    data class Breathing(val patternId: String) : QuickStartTarget()
    data class Toolkit(val toolId: ToolkitToolId) : QuickStartTarget()

    fun encode(): String = when (this) {
        Timer -> KEY_TIMER
        Affirmations -> KEY_AFFIRMATIONS
        Visuals -> KEY_VISUALS
        is Breathing -> "$KEY_BREATHING_PREFIX$patternId"
        is Toolkit -> "$KEY_TOOLKIT_PREFIX${toolId.name}"
    }

    companion object {
        private const val KEY_TIMER = "timer"
        private const val KEY_AFFIRMATIONS = "affirmations"
        private const val KEY_VISUALS = "visuals"
        private const val KEY_BREATHING_PREFIX = "breathing:"
        private const val KEY_TOOLKIT_PREFIX = "toolkit:"

        fun decode(raw: String): QuickStartTarget? {
            val key = raw.trim()
            if (key.isEmpty()) return null
            return when {
                key.equals(KEY_TIMER, ignoreCase = true) -> Timer
                key.equals(KEY_AFFIRMATIONS, ignoreCase = true) -> Affirmations
                key.equals(KEY_VISUALS, ignoreCase = true) -> Visuals
                key.startsWith(KEY_BREATHING_PREFIX, ignoreCase = true) -> {
                    val patternId = key.substringAfter(':')
                    if (BreathingPattern.All.any { it.id == patternId }) {
                        Breathing(patternId)
                    } else {
                        null
                    }
                }
                key.startsWith(KEY_TOOLKIT_PREFIX, ignoreCase = true) -> {
                    val toolName = key.substringAfter(':')
                    runCatching { Toolkit(ToolkitToolId.valueOf(toolName)) }.getOrNull()
                }
                else -> decodeLegacy(key)
            }
        }

        private fun decodeLegacy(legacy: String): QuickStartTarget? = when (legacy.uppercase()) {
            "TIMER" -> Timer
            "AFFIRMATIONS" -> Affirmations
            "VISUALS" -> Visuals
            "BREATHING" -> Breathing(BreathingPattern.BoxBreathing.id)
            "TOOLKIT" -> Toolkit(ToolkitToolId.ThoughtDump)
            else -> null
        }

        fun parseList(stored: String?): List<QuickStartTarget> {
            if (stored.isNullOrBlank()) return emptyList()
            return stored.split(",")
                .mapNotNull { decode(it) }
        }
    }
}
