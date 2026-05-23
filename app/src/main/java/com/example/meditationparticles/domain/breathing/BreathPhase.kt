package com.example.meditationparticles.domain.breathing

enum class BreathPhase {
    Prepare,
    Inhale,
    SecondInhale,
    HoldIn,
    Exhale,
    HoldOut,
    Complete,
}

enum class SessionMode {
    Duration,
    Repetitions,
}

data class BreathingSessionState(
    val pattern: BreathingPattern = BreathingPattern.BoxBreathing,
    val phase: BreathPhase = BreathPhase.Prepare,
    val phaseProgress: Float = 0f,
    val cycleCount: Int = 0,
    val elapsedSessionMs: Long = 0L,
    val isRunning: Boolean = false,
    val sessionMode: SessionMode = SessionMode.Duration,
    val targetMinutes: Int = 5,
    val targetRepetitions: Int = 8,
) {
    val phaseLabel: String
        get() = when (phase) {
            BreathPhase.Prepare -> "Prepare"
            BreathPhase.Inhale -> if (pattern.secondInhaleSeconds > 0f) "Inhale" else "Inhale"
            BreathPhase.SecondInhale -> "Inhale Again"
            BreathPhase.HoldIn -> "Hold"
            BreathPhase.Exhale -> "Exhale"
            BreathPhase.HoldOut -> "Hold"
            BreathPhase.Complete -> "Complete"
        }

    val phaseDescription: String
        get() = when (phase) {
            BreathPhase.Prepare -> "Find a comfortable position and focus on the flow."
            BreathPhase.Inhale, BreathPhase.SecondInhale -> "Breathe in slowly through your nose."
            BreathPhase.HoldIn, BreathPhase.HoldOut -> "Hold gently. Stay present."
            BreathPhase.Exhale -> "Release slowly through your mouth."
            BreathPhase.Complete -> "Session complete. Notice how you feel."
        }

    val phaseDurationSeconds: Float
        get() = when (phase) {
            BreathPhase.Prepare -> 2f
            BreathPhase.Inhale -> pattern.inhaleSeconds
            BreathPhase.SecondInhale -> pattern.secondInhaleSeconds
            BreathPhase.HoldIn -> pattern.holdAfterInhaleSeconds
            BreathPhase.Exhale -> pattern.exhaleSeconds
            BreathPhase.HoldOut -> pattern.holdAfterExhaleSeconds
            BreathPhase.Complete -> 0f
        }

    val secondsRemainingInPhase: Int
        get() {
            val duration = phaseDurationSeconds
            if (duration <= 0f) return 0
            return ((1f - phaseProgress) * duration).toInt().coerceAtLeast(0)
        }

    companion object {
        fun initial() = BreathingSessionState()
    }
}
