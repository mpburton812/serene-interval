package com.example.meditationparticles.ui.home

import java.util.concurrent.TimeUnit

fun formatSessionDuration(seconds: Int): String {
    val minutes = seconds / 60
    return when {
        minutes <= 0 -> "< 1 min"
        minutes == 1 -> "1 min"
        else -> "$minutes mins"
    }
}

fun formatRelativeSessionTime(completedAtMs: Long): String {
    val diffMs = (System.currentTimeMillis() - completedAtMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 120 -> "1 hour ago"
        minutes < 24 * 60 -> "${minutes / 60} hours ago"
        minutes < 48 * 60 -> "Yesterday"
        minutes < 7 * 24 * 60 -> "${minutes / (24 * 60)} days ago"
        else -> "This week"
    }
}
