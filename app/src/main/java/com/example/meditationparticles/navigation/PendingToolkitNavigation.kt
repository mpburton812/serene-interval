package com.example.meditationparticles.navigation

import android.content.Intent
import com.example.meditationparticles.domain.toolkit.ToolkitToolId

data class PendingToolkitNavigation(
    val toolkitTab: String? = null,
    val toolId: ToolkitToolId? = null,
    val futureSelfMessageId: Long? = null,
)

fun Intent.toPendingToolkitNavigation(): PendingToolkitNavigation {
    val tab = getStringExtra(NavigationIntentExtras.OPEN_TOOLKIT_TAB)
    val toolIdName = getStringExtra(NavigationIntentExtras.OPEN_TOOLKIT_TOOL_ID)
    val toolId = toolIdName?.let { name ->
        runCatching { ToolkitToolId.valueOf(name) }.getOrNull()
    }
    val messageId = getLongExtra(NavigationIntentExtras.FUTURE_SELF_MESSAGE_ID, -1L)
        .takeIf { it > 0L }
    return PendingToolkitNavigation(
        toolkitTab = tab,
        toolId = toolId,
        futureSelfMessageId = messageId,
    )
}

fun Intent.clearToolkitNavigationExtras() {
    removeExtra(NavigationIntentExtras.OPEN_TOOLKIT_TAB)
    removeExtra(NavigationIntentExtras.OPEN_TOOLKIT_TOOL_ID)
    removeExtra(NavigationIntentExtras.FUTURE_SELF_MESSAGE_ID)
}
