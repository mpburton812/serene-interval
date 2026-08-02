package com.safehaven.affirmations.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind

@Entity(tableName = "affirmations")
data class AffirmationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val listKind: String = AffirmationListKind.Affirmations.name,
    val isArchived: Boolean = false,
)
