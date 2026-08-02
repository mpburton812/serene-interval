package com.safehaven.affirmations.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "living_tree_people",
    indices = [Index(value = ["name"], unique = true)],
)
data class LivingTreePersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val notes: String = "",
    val sortOrder: Int = 0,
    val angleRadians: Double? = null,
    val radiusFraction: Double? = null,
    val isUserPlaced: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)
