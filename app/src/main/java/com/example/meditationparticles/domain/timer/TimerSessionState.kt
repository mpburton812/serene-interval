package com.example.meditationparticles.domain.timer

data class TimerSessionState(
    val displayMode: TimerDisplayMode = TimerDisplayMode.Hourglass,
    val phase: TimerPhase = TimerPhase.Idle,
    val targetMinutes: Int = TimerPresets.DEFAULT_MINUTES,
    val elapsedMs: Long = 0L,
    val prepareElapsedMs: Long = 0L,
    val isRunning: Boolean = false,
    val sound: TimerSoundOption = TimerSoundOption.None,
    val customSoundUri: String? = null,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
) {
    val totalMs: Long get() = targetMinutes * 60_000L
    val progress: Float
        get() = if (totalMs <= 0L) 0f else (elapsedMs.toFloat() / totalMs).coerceIn(0f, 1f)

    val remainingMs: Long get() = (totalMs - elapsedMs).coerceAtLeast(0L)

    val remainingFormatted: String
        get() {
            val totalSec = (remainingMs / 1000L).toInt()
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            return "%02d:%02d".format(minutes, seconds)
        }

    val statusLabel: String
        get() = when (phase) {
            TimerPhase.Complete -> "Complete"
            else -> ""
        }

    val statusDescription: String
        get() = when (phase) {
            TimerPhase.Idle -> "Choose a duration and display mode, then begin."
            TimerPhase.Complete -> "Session complete. Take a moment before returning."
            else -> ""
        }

    val prepareCountdownSeconds: Int
        get() {
            if (phase != TimerPhase.Prepare) return 0
            val remainingMs = (TimerPrepareTiming.COUNTDOWN_MS - prepareElapsedMs).coerceAtLeast(0L)
            return ((remainingMs + 999) / 1_000).toInt().coerceIn(1, 5)
        }

    val isPrepareBeginVisible: Boolean
        get() = phase == TimerPhase.Prepare &&
            prepareElapsedMs >= TimerPrepareTiming.COUNTDOWN_MS &&
            prepareElapsedMs < TimerPrepareTiming.totalMs

    companion object {
        fun initial() = TimerSessionState()
    }
}
