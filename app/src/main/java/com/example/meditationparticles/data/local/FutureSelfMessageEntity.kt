package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "future_self_messages")
data class FutureSelfMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val audioPath: String? = null,
    val scheduledAtMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val delivered: Boolean = false,
)
