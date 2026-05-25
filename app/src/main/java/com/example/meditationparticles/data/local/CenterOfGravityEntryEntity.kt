package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "center_of_gravity_entries")
data class CenterOfGravityEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val thoughtsAndFeelings: String,
    val thoughtsAndFeelingsAudioPath: String? = null,
    val bodyAndNeeds: String,
    val bodyAndNeedsAudioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
