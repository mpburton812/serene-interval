package com.example.meditationparticles.ui.sanctuary

enum class SanctuaryWalkthroughStep(val title: String) {
    Welcome("Welcome"),
    Name("Name your Sway"),
    Appearance("Appearance"),
    Spaces("Your spaces"),
    QuickStart("Quick Start"),
    Toolkit("Toolkit"),
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
