package com.safehaven.affirmations.domain.toolkit

data class HeartsStepDefinition(
    val label: String,
    val hint: String,
    val minLines: Int = 3,
)

object HeartsToolConfig {
    fun stepCount(toolId: ToolkitToolId): Int = steps(toolId).size

    fun steps(toolId: ToolkitToolId): List<HeartsStepDefinition> = when (toolId) {
        ToolkitToolId.DelightDeposit -> listOf(
            HeartsStepDefinition("Who or what", "Name a person, connection, or yourself."),
            HeartsStepDefinition("What delighted you", "What specifically brought joy or appreciation?"),
            HeartsStepDefinition("How you'll express it", "A message, gesture, or moment you'll offer."),
        )
        ToolkitToolId.AttunementMap -> listOf(
            HeartsStepDefinition("What I observed", "Facts only — what happened without judgment."),
            HeartsStepDefinition("What they might feel or need", "A gentle hypothesis, not a verdict."),
            HeartsStepDefinition("What I feel and need", "Your inner response in this moment."),
            HeartsStepDefinition("A curious question", "One question you could ask to understand better."),
        )
        ToolkitToolId.RepairReconnect -> listOf(
            HeartsStepDefinition("What happened", "Brief facts about the rupture or tension."),
            HeartsStepDefinition("Their possible experience", "How this might have landed for them."),
            HeartsStepDefinition("Repair I can offer", "What you'd like to acknowledge or apologize for."),
            HeartsStepDefinition("Reconnection", "What turning toward could look like now."),
        )
        ToolkitToolId.SecureSelfCheck -> listOf(
            HeartsStepDefinition("What I'm feeling", "Name emotions without fixing them yet."),
            HeartsStepDefinition("What I need", "Comfort, clarity, boundary, connection, or rest."),
            HeartsStepDefinition("How I'll offer it to myself", "One kind action from your secure self."),
        )
        ToolkitToolId.AppreciationRitual -> listOf(
            HeartsStepDefinition("Connection", "Who are you appreciating this week?"),
            HeartsStepDefinition("Delight one", "First specific delight about them."),
            HeartsStepDefinition("Delight two", "Second delight."),
            HeartsStepDefinition("Delight three", "Third delight."),
        )
        ToolkitToolId.NeedsBeforeNegotiation -> listOf(
            HeartsStepDefinition("My needs", "What matters to you in this conversation."),
            HeartsStepDefinition("Their possible needs", "What they might be protecting or wanting."),
            HeartsStepDefinition("Shared goal", "One aim you both could move toward."),
        )
        ToolkitToolId.AttachmentStorySnapshot -> listOf(
            HeartsStepDefinition("Early memory", "A formative moment in how you learned attachment."),
            HeartsStepDefinition("Pattern noticed", "What you tend to do in close relationships."),
            HeartsStepDefinition("What I needed then", "What was missing or longed for."),
            HeartsStepDefinition("What I need now", "What your present self needs to feel secure."),
            HeartsStepDefinition("Kinder narrative", "One compassionate line about your story."),
        )
        else -> emptyList()
    }

    fun supportsPersonField(toolId: ToolkitToolId): Boolean = toolId in setOf(
        ToolkitToolId.DelightDeposit,
        ToolkitToolId.AttunementMap,
        ToolkitToolId.RepairReconnect,
        ToolkitToolId.AppreciationRitual,
        ToolkitToolId.NeedsBeforeNegotiation,
    )

    fun isJournalTool(toolId: ToolkitToolId): Boolean = stepCount(toolId) > 0

    fun journalToolIds(): Set<ToolkitToolId> = setOf(
        ToolkitToolId.DelightDeposit,
        ToolkitToolId.AttunementMap,
        ToolkitToolId.RepairReconnect,
        ToolkitToolId.SecureSelfCheck,
        ToolkitToolId.AppreciationRitual,
        ToolkitToolId.NeedsBeforeNegotiation,
        ToolkitToolId.AttachmentStorySnapshot,
    )
}
