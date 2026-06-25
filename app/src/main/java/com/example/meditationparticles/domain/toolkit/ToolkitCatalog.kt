package com.example.meditationparticles.domain.toolkit

enum class ToolkitCategory {
    Proactive,
    Reactive,
}

enum class ToolkitToolId {
    ThoughtDump,
    BoundarySetting,
    MicroPause,
    FutureSelfMessage,
    Grounding54321,
    MuscleRelaxation,
    LovingKindness,
    AnxietyLog,
    Refactoring,
    NonViolentCommunication,
    RelocateCenterOfGravity,
    DelightDeposit,
    AttunementMap,
    RepairReconnect,
    SecureSelfCheck,
    PresenceTimer,
    AppreciationRitual,
    NeedsBeforeNegotiation,
    AttachmentStorySnapshot,
    HeartsFlowerPartners,
}

data class ToolkitTool(
    val id: ToolkitToolId,
    val title: String,
    val description: String,
    val category: ToolkitCategory,
    val lane: ToolkitLane = ToolkitLane.Core,
    val steps: List<String>,
)

object ToolkitCatalog {
    val all: List<ToolkitTool> = listOf(
        ToolkitTool(
            id = ToolkitToolId.ThoughtDump,
            title = "Capture Thought",
            description = "Jot down a thought and mood.",
            category = ToolkitCategory.Proactive,
            steps = listOf(
                "Find a quiet moment. This is a judgment-free space.",
                "Write every worry, task, and nagging thought — don't edit.",
                "Keep going until your mind feels lighter.",
                "When finished, take one slow breath and close the page.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.BoundarySetting,
            title = "Boundary Setting",
            description = "A guide to protecting your mental space.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Notice what is draining your energy right now.",
                "Name one boundary you need — time, space, or emotional.",
                "Write one kind but clear sentence you could say.",
                "Choose one small action to protect that boundary today.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.MicroPause,
            title = "Micro-Pause Practice",
            description = "30-second resets throughout your day.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Unclench your jaw and let your tongue rest softly.",
                "Drop your shoulders away from your ears.",
                "Feel your feet on the ground beneath you.",
                "Take one slow breath in… and a longer breath out.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.FutureSelfMessage,
            title = "Give Message To Future Self",
            description = "Write or record a note your future self will receive.",
            category = ToolkitCategory.Proactive,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.Grounding54321,
            title = "5-4-3-2-1 Grounding",
            description = "Engage your senses to return to the now.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Name 5 things you can see around you.",
                "Name 4 things you can touch or feel.",
                "Name 3 things you can hear right now.",
                "Name 2 things you can smell.",
                "Name 1 thing you can taste.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.MuscleRelaxation,
            title = "Muscle Relaxation",
            description = "Step-by-step tension release.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Tense your feet for 5 seconds, then release completely.",
                "Tense your legs and hips, hold, then soften.",
                "Tense your stomach and chest, hold, then let go.",
                "Tense your hands and arms, hold, then release.",
                "Tense your shoulders and face, hold, then melt into ease.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.LovingKindness,
            title = "Loving Kindness",
            description = "Send warmth to yourself and others.",
            category = ToolkitCategory.Proactive,
            steps = listOf(
                "Find a comfortable posture. Place a hand on your heart if that feels grounding.",
                "Silently repeat: May I be safe. May I be happy. May I be healthy. May I live with ease.",
                "Bring someone you care about to mind. Repeat: May you be safe. May you be happy. May you be healthy. May you live with ease.",
                "Extend this wish to all beings: May all beings be safe, happy, healthy, and at ease.",
                "Notice how your body feels. Carry this warmth with you as you return.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.AnxietyLog,
            title = "Anxiety Log",
            description = "Notice, observe, and acknowledge what you feel.",
            category = ToolkitCategory.Reactive,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.Refactoring,
            title = "Refactoring",
            description = "Separate interpretation from facts and explore calmer explanations.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Write down the actual facts — only what you know for certain.",
                "Write down your interpretation — the story your mind is telling.",
                "Write three non-threatening explanations based on logic.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.NonViolentCommunication,
            title = "Non-Violent Communication",
            description = "Express yourself clearly with observation, feeling, need, and request.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Describe what happened — just the facts, without judgment.",
                "Name the feeling this brings up in you.",
                "What need or value of yours is connected to this feeling?",
                "What clear, specific request could help meet that need?",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.RelocateCenterOfGravity,
            title = "Relocate Center of Gravity",
            description = "Return attention from your partner to your own inner ground.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "What am I thinking and feeling right now?",
                "What am I feeling in my body right now? What do I need in this moment to feel slightly more grounded?",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.DelightDeposit,
            title = "Delight Deposit",
            description = "Name specific joy and how you'll express appreciation.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.AttunementMap,
            title = "Attunement Map",
            description = "Map observations, hypotheses, and curious questions before speaking.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.SecureSelfCheck,
            title = "Secure Self Check",
            description = "Offer yourself the comfort and care you need right now.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.AppreciationRitual,
            title = "Appreciation Ritual",
            description = "Weekly practice of naming three delights about a connection.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.AttachmentStorySnapshot,
            title = "Attachment Story Snapshot",
            description = "Gently re-author your attachment history with compassion.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.HeartsFlowerPartners,
            title = "Partner HEARTS Touchpoints",
            description = "See your Flower partners and recent HEARTS practices with them.",
            category = ToolkitCategory.Proactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.RepairReconnect,
            title = "Repair & Reconnect",
            description = "Turn toward reconnection after conflict or distance.",
            category = ToolkitCategory.Reactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
        ToolkitTool(
            id = ToolkitToolId.PresenceTimer,
            title = "Presence Timer",
            description = "A brief reset to be fully here with yourself or someone else.",
            category = ToolkitCategory.Reactive,
            lane = ToolkitLane.Hearts,
            steps = listOf(
                "Set your device aside or turn it face-down.",
                "Take one slow breath. Feel your feet and your seat.",
                "Ask: Who am I with right now — or who is on my heart?",
                "Choose one sentence of presence you can offer in the next moment.",
            ),
        ),
        ToolkitTool(
            id = ToolkitToolId.NeedsBeforeNegotiation,
            title = "Needs Before Negotiation",
            description = "Clarify needs before a hard conversation.",
            category = ToolkitCategory.Reactive,
            lane = ToolkitLane.Hearts,
            steps = emptyList(),
        ),
    )

    fun byId(id: ToolkitToolId): ToolkitTool? = all.find { it.id == id }

    fun byCategory(category: ToolkitCategory, lane: ToolkitLane = ToolkitLane.Core): List<ToolkitTool> =
        all.filter { it.category == category && it.lane == lane }

    fun byLane(lane: ToolkitLane): List<ToolkitTool> = all.filter { it.lane == lane }

    fun randomReactive(): ToolkitTool? =
        byCategory(ToolkitCategory.Reactive).randomOrNull()
}
