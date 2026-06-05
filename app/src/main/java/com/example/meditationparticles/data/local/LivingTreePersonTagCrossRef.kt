package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "living_tree_person_tags",
    primaryKeys = ["personId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = LivingTreePersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LivingTreeTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tagId")],
)
data class LivingTreePersonTagCrossRef(
    val personId: Long,
    val tagId: Long,
)
