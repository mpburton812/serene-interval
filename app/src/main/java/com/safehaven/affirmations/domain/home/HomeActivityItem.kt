package com.safehaven.affirmations.domain.home

data class HomeActivityItem(
    val id: String,
    val completedAt: Long,
    val title: String,
    val subtitle: String? = null,
    val textEntry: String? = null,
    val moodLevel: Int? = null,
)
