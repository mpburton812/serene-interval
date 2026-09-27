package com.safehaven.affirmations.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "thermometers")
data class ThermometerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "thermometer_events",
    foreignKeys = [
        ForeignKey(
            entity = ThermometerEntity::class,
            parentColumns = ["id"],
            childColumns = ["thermometerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("thermometerId")],
)
data class ThermometerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val thermometerId: Long,
    val type: String,
    val score: Int? = null,
    val note: String? = null,
    val previousName: String? = null,
    val newName: String? = null,
    val recordedAt: Long,
)

object ThermometerEventType {
    const val READING = "READING"
    const val RENAME = "RENAME"
}
