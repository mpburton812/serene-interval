package com.safehaven.affirmations.ui.sanctuary

enum class SanctuaryWalkthroughStep(val title: String) {
    Welcome("Welcome"),
    Name("Name your SafeHaven"),
    Appearance("Appearance"),
    Spaces("Your spaces"),
    Toolkit("Toolkit"),
    QuickStart("Quick Start"),
    Review("Review"),
    ;

    companion object {
        val ordered: List<SanctuaryWalkthroughStep> = entries.toList()
    }
}

enum class SanctuaryWalkthroughMode {
    FirstVisit,
    Remodel,
}
