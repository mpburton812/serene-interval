package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.meditationparticles.domain.toolkit.ToolkitToolId

@Entity(tableName = "hearts_entries")
data class HeartsEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val toolId: String,
    val personId: Long? = null,
    val personName: String = "",
    val step1: String = "",
    val step2: String = "",
    val step3: String = "",
    val step4: String = "",
    val step5: String = "",
    val moodLevel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun stepValues(): List<String> = listOf(step1, step2, step3, step4, step5)

    companion object {
        fun fromDraft(
            toolId: ToolkitToolId,
            personId: Long?,
            personName: String,
            steps: List<String>,
            moodLevel: Int?,
        ): HeartsEntryEntity = HeartsEntryEntity(
            toolId = toolId.name,
            personId = personId,
            personName = personName.trim(),
            step1 = steps.getOrElse(0) { "" }.trim(),
            step2 = steps.getOrElse(1) { "" }.trim(),
            step3 = steps.getOrElse(2) { "" }.trim(),
            step4 = steps.getOrElse(3) { "" }.trim(),
            step5 = steps.getOrElse(4) { "" }.trim(),
            moodLevel = moodLevel,
        )
    }
}
