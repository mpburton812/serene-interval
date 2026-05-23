package com.example.meditationparticles.domain.breathing

data class BreathingPattern(
    val id: String,
    val name: String,
    val inhaleSeconds: Float,
    val holdAfterInhaleSeconds: Float,
    val exhaleSeconds: Float,
    val holdAfterExhaleSeconds: Float,
    val purpose: String,
    /** Second inhale top-off for Physiological Sigh (seconds). */
    val secondInhaleSeconds: Float = 0f,
) {
    val patternLabel: String
        get() = if (secondInhaleSeconds > 0f) {
            "Double Inhale : 0 : Long Exhale : 0"
        } else {
            "${inhaleSeconds.toInt()} : ${holdAfterInhaleSeconds.toInt()} : " +
                "${exhaleSeconds.toInt()} : ${holdAfterExhaleSeconds.toInt()}"
        }

    companion object {
        val BoxBreathing = BreathingPattern(
            id = "box",
            name = "Box Breathing",
            inhaleSeconds = 4f,
            holdAfterInhaleSeconds = 4f,
            exhaleSeconds = 4f,
            holdAfterExhaleSeconds = 4f,
            purpose = "Calms a racing mind and resets your focus when overwhelmed or stressed.",
        )

        val FourSevenEight = BreathingPattern(
            id = "478",
            name = "4-7-8 Technique",
            inhaleSeconds = 4f,
            holdAfterInhaleSeconds = 7f,
            exhaleSeconds = 8f,
            holdAfterExhaleSeconds = 0f,
            purpose = "Acts as a natural tranquilizer to help you fall asleep or stop anxiety quickly.",
        )

        val Resonant = BreathingPattern(
            id = "resonant",
            name = "Resonant Breathing",
            inhaleSeconds = 5f,
            holdAfterInhaleSeconds = 0f,
            exhaleSeconds = 5f,
            holdAfterExhaleSeconds = 0f,
            purpose = "Balances heart rate and breathing to lower daily stress.",
        )

        val Tactical = BreathingPattern(
            id = "tactical",
            name = "Tactical Breathing",
            inhaleSeconds = 4f,
            holdAfterInhaleSeconds = 4f,
            exhaleSeconds = 4f,
            holdAfterExhaleSeconds = 0f,
            purpose = "Quickly grounds you during sudden panic or high pressure.",
        )

        val SamaVritti = BreathingPattern(
            id = "sama_vritti",
            name = "Sama Vritti",
            inhaleSeconds = 4f,
            holdAfterInhaleSeconds = 0f,
            exhaleSeconds = 4f,
            holdAfterExhaleSeconds = 0f,
            purpose = "Evens out breathing rhythm to steady energy before meditating.",
        )

        val PhysiologicalSigh = BreathingPattern(
            id = "physiological_sigh",
            name = "Physiological Sigh",
            inhaleSeconds = 1.5f,
            holdAfterInhaleSeconds = 0f,
            exhaleSeconds = 6f,
            holdAfterExhaleSeconds = 0f,
            secondInhaleSeconds = 0.5f,
            purpose = "The fastest way to lower stress — dumps CO₂ to reduce heart rate.",
        )

        val All = listOf(
            BoxBreathing,
            FourSevenEight,
            Resonant,
            Tactical,
            SamaVritti,
            PhysiologicalSigh,
        )

        fun byId(id: String): BreathingPattern =
            All.firstOrNull { it.id == id } ?: BoxBreathing
    }
}
