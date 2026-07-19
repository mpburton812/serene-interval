package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.meditationparticles.domain.affirmations.AffirmationListKind

@Entity(tableName = "affirmations")
data class AffirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val listKind: String = AffirmationListKind.Affirmations.name,
    val archived: Boolean = false,
)
