package com.example.meditationparticles.domain.toolkit

enum class ToolkitCategory {
    Proactive,
    Reactive,
}

enum class ToolkitToolId {
    ThoughtDump,
    BoundarySetting,
    MicroPause,
    Grounding54321,
    BoxBreathing,
    MuscleRelaxation,
    LovingKindness,
    AnxietyLog,
}

data class ToolkitTool(
    val id: ToolkitToolId,
    val title: String,
    val description: String,
    val category: ToolkitCategory,
    val steps: List<String>,
)

object ToolkitCatalog {
    val all: List<ToolkitTool> = listOf(
        ToolkitTool(
            id = ToolkitToolId.ThoughtDump,
            title = "Daily Thought Dump",
            description = "Clear cognitive clutter before it builds.",
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
            category = ToolkitCategory.Proactive,
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
            category = ToolkitCategory.Proactive,
            steps = listOf(
                "Unclench your jaw and let your tongue rest softly.",
                "Drop your shoulders away from your ears.",
                "Feel your feet on the ground beneath you.",
                "Take one slow breath in… and a longer breath out.",
            ),
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
            id = ToolkitToolId.BoxBreathing,
            title = "Box Breathing",
            description = "Immediate nervous system regulation.",
            category = ToolkitCategory.Reactive,
            steps = listOf(
                "Inhale slowly through your nose for 4 counts.",
                "Hold gently for 4 counts.",
                "Exhale slowly through your mouth for 4 counts.",
                "Hold empty for 4 counts, then repeat.",
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
            category = ToolkitCategory.Reactive,
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
    )

    fun byId(id: ToolkitToolId): ToolkitTool? = all.find { it.id == id }

    fun byCategory(category: ToolkitCategory): List<ToolkitTool> =
        all.filter { it.category == category }

    fun randomReactive(): ToolkitTool? =
        byCategory(ToolkitCategory.Reactive).randomOrNull()
}
