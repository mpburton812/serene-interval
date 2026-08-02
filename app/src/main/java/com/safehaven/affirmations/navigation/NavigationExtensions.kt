package com.safehaven.affirmations.navigation

fun SereneDestination.navigationRoute(toolkitTab: String? = null): String = when (this) {
    SereneDestination.Toolkit -> SereneDestination.Toolkit.route
    SereneDestination.Affirmations -> SereneDestination.Affirmations.route
    SereneDestination.KatiesLoveList -> SereneDestination.KatiesLoveList.route
    else -> route
}

fun SereneDestination.navigationRouteFromLegacyTab(toolkitTab: String?): String = when (toolkitTab) {
    SereneDestination.ToolkitTab.AFFIRMATIONS -> SereneDestination.Affirmations.route
    SereneDestination.ToolkitTab.KATIES_LOVE_LIST -> SereneDestination.KatiesLoveList.route
    SereneDestination.ToolkitTab.TOOLKIT -> SereneDestination.Toolkit.route
    else -> route
}
