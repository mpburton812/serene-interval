package com.example.meditationparticles.domain.timer

object TimerPrepareTiming {
    const val COUNTDOWN_MS = 10_000L
    const val BEGIN_VISIBLE_MS = 1_200L
    const val BEGIN_FADE_MS = 650L
    val totalMs: Long = COUNTDOWN_MS + BEGIN_VISIBLE_MS + BEGIN_FADE_MS
}
