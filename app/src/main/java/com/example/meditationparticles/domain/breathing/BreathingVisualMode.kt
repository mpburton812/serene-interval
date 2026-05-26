package com.example.meditationparticles.domain.breathing

enum class BreathingVisualMode(val label: String) {
    /** Diamond/vertical layout, phase-duration fill, cycle-aware fluid. */
    A("A"),
    /** Legacy InterleavedLadder/FlowChain layout with 1s segments. */
    B("B"),
}
