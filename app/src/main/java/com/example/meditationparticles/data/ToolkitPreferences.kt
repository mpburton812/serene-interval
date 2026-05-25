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

    companion object {
        private const val PREFS_NAME = "toolkit_preferences"
        private const val KEY_CONFIGURED = "toolkit_configured"
        private const val KEY_ENABLED_TOOLS = "enabled_tool_ids"
        private const val KEY_PROACTIVE_ORDER = "proactive_order"
        private const val KEY_REACTIVE_ORDER = "reactive_order"
    }
}
