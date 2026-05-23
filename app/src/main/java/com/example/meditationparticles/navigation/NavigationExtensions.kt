package com.example.meditationparticles.navigation

fun SereneDestination.navigationRoute(toolkitTab: String? = null): String = when (this) {
    SereneDestination.Toolkit -> SereneDestination.Toolkit.createRoute(
        toolkitTab ?: SereneDestination.ToolkitTab.AFFIRMATIONS,
    )
    else -> route
}
