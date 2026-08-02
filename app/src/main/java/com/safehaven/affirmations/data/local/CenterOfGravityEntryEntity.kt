package com.safehaven.affirmations.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "center_of_gravity_entries")
data class CenterOfGravityEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val thoughtsAndFeelings: String,
    val bodyAndNeeds: String,
    val moodLevel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
