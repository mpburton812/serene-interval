package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "living_tree_tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class LivingTreeTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorArgb: Int,
    val sortOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
