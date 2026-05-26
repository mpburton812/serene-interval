package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ToolkitPrefsSnapshot(
    val configured: Boolean,
    val enabledToolIds: Set<ToolkitToolId>,
    val proactiveOrder: List<ToolkitToolId>,
    val reactiveOrder: List<ToolkitToolId>,
    val usageCounts: Map<ToolkitToolId, Int> = emptyMap(),
) {
    val hasAnyToolEnabled: Boolean get() = enabledToolIds.isNotEmpty()
}

class ToolkitPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _snapshot = MutableStateFlow(load(onboardingCompleted = false))
    val snapshot: StateFlow<ToolkitPrefsSnapshot> = _snapshot.asStateFlow()

    fun load(onboardingCompleted: Boolean): ToolkitPrefsSnapshot {
        val configured = resolveConfigured(onboardingCompleted)
        val enabledToolIds = readEnabledToolIds()
        return ToolkitPrefsSnapshot(
            configured = configured,
            enabledToolIds = enabledToolIds,
            proactiveOrder = readOrder(
                key = KEY_PROACTIVE_ORDER,
                category = ToolkitCategory.Proactive,
            ),
            reactiveOrder = readOrder(
                key = KEY_REACTIVE_ORDER,
                category = ToolkitCategory.Reactive,
            ),
            usageCounts = readUsageCounts(),
        )
    }

    fun refresh(onboardingCompleted: Boolean) {
        _snapshot.value = load(onboardingCompleted)
    }

    fun saveConfiguration(enabledToolIds: Set<ToolkitToolId>) {
        val normalizedEnabled = enabledToolIds.ifEmpty { ToolkitLayout.defaultEnabledTools() }
        prefs.edit()
            .putBoolean(KEY_CONFIGURED, true)
            .putStringSet(KEY_ENABLED_TOOLS, normalizedEnabled.map { it.name }.toSet())
            .apply()
        _snapshot.update {
            it.copy(
                configured = true,
                enabledToolIds = normalizedEnabled,
            )
        }
    }

    fun setEnabledTools(enabledToolIds: Set<ToolkitToolId>) {
        prefs.edit()
            .putStringSet(KEY_ENABLED_TOOLS, enabledToolIds.map { it.name }.toSet())
            .apply()
        _snapshot.update { it.copy(enabledToolIds = enabledToolIds) }
    }

    fun saveProactiveOrder(order: List<ToolkitToolId>) {
        val normalized = ToolkitLayout.normalizeOrder(ToolkitCategory.Proactive, order)
        prefs.edit()
            .putString(KEY_PROACTIVE_ORDER, normalized.joinToString(",") { it.name })
            .apply()
        _snapshot.update { it.copy(proactiveOrder = normalized) }
    }

    fun saveReactiveOrder(order: List<ToolkitToolId>) {
        val normalized = ToolkitLayout.normalizeOrder(ToolkitCategory.Reactive, order)
        prefs.edit()
            .putString(KEY_REACTIVE_ORDER, normalized.joinToString(",") { it.name })
            .apply()
        _snapshot.update { it.copy(reactiveOrder = normalized) }
    }

    fun incrementUsageCount(toolId: ToolkitToolId) {
        val current = readUsageCounts().toMutableMap()
        current[toolId] = (current[toolId] ?: 0) + 1
        writeUsageCounts(current)
        _snapshot.update { it.copy(usageCounts = current) }
    }

    fun saveUsageCounts(counts: Map<ToolkitToolId, Int>) {
        val normalized = counts.filterValues { it > 0 }
        writeUsageCounts(normalized)
        _snapshot.update { it.copy(usageCounts = normalized) }
    }

    private fun resolveConfigured(onboardingCompleted: Boolean): Boolean {
        if (!prefs.contains(KEY_CONFIGURED)) {
            if (onboardingCompleted) {
                saveConfigurationSilently(ToolkitLayout.defaultEnabledTools())
                return true
            }
            return false
        }
        return prefs.getBoolean(KEY_CONFIGURED, false)
    }

    fun markNeedsConfiguration() {
        prefs.edit()
            .putBoolean(KEY_CONFIGURED, false)
            .apply()
        _snapshot.update { it.copy(configured = false) }
    }

    private fun saveConfigurationSilently(enabledToolIds: Set<ToolkitToolId>) {
        val normalizedEnabled = enabledToolIds.ifEmpty { ToolkitLayout.defaultEnabledTools() }
        prefs.edit()
            .putBoolean(KEY_CONFIGURED, true)
            .putStringSet(KEY_ENABLED_TOOLS, normalizedEnabled.map { it.name }.toSet())
            .apply()
    }

    private fun readEnabledToolIds(): Set<ToolkitToolId> {
        val stored = prefs.getStringSet(KEY_ENABLED_TOOLS, null)
            ?: return ToolkitLayout.defaultEnabledTools()
        return stored.mapNotNull { name ->
            runCatching { ToolkitToolId.valueOf(name) }.getOrNull()
        }.toSet().ifEmpty { ToolkitLayout.defaultEnabledTools() }
    }

    private fun readOrder(key: String, category: ToolkitCategory): List<ToolkitToolId> {
        val raw = prefs.getString(key, null) ?: return ToolkitLayout.defaultOrder(category)
        if (raw.isBlank()) return ToolkitLayout.defaultOrder(category)
        val parsed = raw.split(",")
            .mapNotNull { name ->
                runCatching { ToolkitToolId.valueOf(name.trim()) }.getOrNull()
            }
        return ToolkitLayout.normalizeOrder(category, parsed)
    }

    private fun readUsageCounts(): Map<ToolkitToolId, Int> {
        val raw = prefs.getString(KEY_USAGE_COUNTS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size != 2) return@mapNotNull null
                val id = runCatching { ToolkitToolId.valueOf(parts[0].trim()) }.getOrNull()
                    ?: return@mapNotNull null
                val count = parts[1].trim().toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
                if (count == 0) return@mapNotNull null
                id to count
            }
            .toMap()
    }

    private fun writeUsageCounts(counts: Map<ToolkitToolId, Int>) {
        val serialized = counts.entries
            .filter { it.value > 0 }
            .joinToString(",") { (id, count) -> "${id.name}=$count" }
        prefs.edit()
            .putString(KEY_USAGE_COUNTS, serialized)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "toolkit_preferences"
        private const val KEY_CONFIGURED = "toolkit_configured"
        private const val KEY_ENABLED_TOOLS = "enabled_tool_ids"
        private const val KEY_PROACTIVE_ORDER = "proactive_order"
        private const val KEY_REACTIVE_ORDER = "reactive_order"
        private const val KEY_USAGE_COUNTS = "tool_usage_counts"
    }
}
