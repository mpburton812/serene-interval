package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.HeartsEntryDao
import com.example.meditationparticles.data.local.HeartsEntryEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import com.example.meditationparticles.domain.toolkit.HeartsToolConfig
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.flow.Flow

class HeartsRepository(
    private val dao: HeartsEntryDao,
    private val moodTracker: MoodTrackerRepository,
) {
    fun observeAll(): Flow<List<HeartsEntryEntity>> = dao.observeAll()

    fun observeByTool(toolId: ToolkitToolId): Flow<List<HeartsEntryEntity>> =
        dao.observeByTool(toolId.name)

    suspend fun getLatestForPerson(personId: Long): HeartsEntryEntity? =
        dao.getLatestForPerson(personId)

    suspend fun save(
        toolId: ToolkitToolId,
        personId: Long?,
        personName: String,
        steps: List<String>,
        moodLevel: Int?,
    ): Long? {
        val entry = HeartsEntryEntity.fromDraft(
            toolId = toolId,
            personId = personId,
            personName = personName,
            steps = steps,
            moodLevel = moodLevel,
        )
        val hasContent = entry.stepValues().any { it.isNotBlank() } ||
            entry.personName.isNotBlank()
        if (!hasContent) return null
        val id = dao.insert(entry)
        MoodScale.normalize(moodLevel)?.let { level ->
            moodTracker.record(
                source = MoodSource.HEARTS_JOURNAL,
                level = level,
                atMillis = entry.createdAt,
                legacyTable = MoodTrackerRepository.TABLE_HEARTS_ENTRIES,
                legacyRowId = id,
            )
        }
        return id
    }

    suspend fun deleteEntry(id: Long) {
        dao.deleteById(id)
    }

    fun toolTitle(toolId: ToolkitToolId): String = when (toolId) {
        ToolkitToolId.DelightDeposit -> "Delight Deposit"
        ToolkitToolId.AttunementMap -> "Attunement Map"
        ToolkitToolId.RepairReconnect -> "Repair & Reconnect"
        ToolkitToolId.SecureSelfCheck -> "Secure Self Check"
        ToolkitToolId.AppreciationRitual -> "Appreciation Ritual"
        ToolkitToolId.NeedsBeforeNegotiation -> "Needs Before Negotiation"
        ToolkitToolId.AttachmentStorySnapshot -> "Attachment Story"
        else -> toolId.name
    }

    fun entrySummary(entry: HeartsEntryEntity): String {
        val toolId = runCatching { ToolkitToolId.valueOf(entry.toolId) }.getOrNull()
        val steps = entry.stepValues().filter { it.isNotBlank() }
        val prefix = when {
            entry.personName.isNotBlank() -> "${entry.personName}: "
            else -> ""
        }
        val body = steps.firstOrNull() ?: ""
        val title = toolId?.let { toolTitle(it) } ?: entry.toolId
        return if (body.isBlank()) title else "$prefix$body"
    }

    fun heartsLetter(toolId: ToolkitToolId): Char? = when (toolId) {
        ToolkitToolId.DelightDeposit -> 'E'
        ToolkitToolId.AttunementMap -> 'A'
        ToolkitToolId.RepairReconnect -> 'T'
        ToolkitToolId.SecureSelfCheck -> 'S'
        ToolkitToolId.PresenceTimer -> 'H'
        ToolkitToolId.AppreciationRitual -> 'E'
        ToolkitToolId.NeedsBeforeNegotiation -> 'A'
        ToolkitToolId.AttachmentStorySnapshot -> 'S'
        else -> null
    }
}
