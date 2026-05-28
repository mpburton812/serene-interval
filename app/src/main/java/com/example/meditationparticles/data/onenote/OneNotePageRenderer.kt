package com.example.meditationparticles.data.onenote

import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object OneNotePageRenderer {
    private val displayDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun renderNvc(entry: NvcEntryEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.NonViolentCommunication)
        val steps = tool?.steps.orEmpty()
        val dateLabel = formatDate(entry.createdAt)
        val body = buildString {
            appendLine("<h1>Non-Violent Communication</h1>")
            appendFieldSection(
                stepHint = steps.getOrNull(0),
                heading = "Observation",
                dataId = "observation",
                text = entry.observation,
                audioPath = entry.observationAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(1),
                heading = "Feeling",
                dataId = "feeling",
                text = entry.feeling,
                audioPath = entry.feelingAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(2),
                heading = "Need",
                dataId = "need",
                text = entry.need,
                audioPath = entry.needAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(3),
                heading = "Request",
                dataId = "request",
                text = entry.request,
                audioPath = entry.requestAudioPath,
            )
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "NVC — $dateLabel",
            html = wrapHtml(title = "NVC — $dateLabel", createdAtMillis = entry.createdAt, body = body),
        )
    }

    fun renderRefactoring(entry: RefactoringEntryEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.Refactoring)
        val steps = tool?.steps.orEmpty()
        val dateLabel = formatDate(entry.createdAt)
        val body = buildString {
            appendLine("<h1>Refactoring</h1>")
            appendFieldSection(
                stepHint = steps.getOrNull(0),
                heading = "Actual facts",
                dataId = "actualFacts",
                text = entry.actualFacts,
                audioPath = entry.actualFactsAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(1),
                heading = "Interpretation",
                dataId = "interpretation",
                text = entry.interpretation,
                audioPath = entry.interpretationAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(2),
                heading = "Explanation 1",
                dataId = "explanation1",
                text = entry.explanation1,
                audioPath = entry.explanation1AudioPath,
            )
            appendFieldSection(
                heading = "Explanation 2",
                dataId = "explanation2",
                text = entry.explanation2,
                audioPath = entry.explanation2AudioPath,
            )
            appendFieldSection(
                heading = "Explanation 3",
                dataId = "explanation3",
                text = entry.explanation3,
                audioPath = entry.explanation3AudioPath,
            )
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Refactoring — $dateLabel",
            html = wrapHtml(title = "Refactoring — $dateLabel", createdAtMillis = entry.createdAt, body = body),
        )
    }

    fun renderCenterOfGravity(entry: CenterOfGravityEntryEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.RelocateCenterOfGravity)
        val steps = tool?.steps.orEmpty()
        val dateLabel = formatDate(entry.createdAt)
        val body = buildString {
            appendLine("<h1>Relocate Center of Gravity</h1>")
            appendFieldSection(
                stepHint = steps.getOrNull(0),
                heading = "Thoughts and feelings",
                dataId = "thoughtsAndFeelings",
                text = entry.thoughtsAndFeelings,
                audioPath = entry.thoughtsAndFeelingsAudioPath,
            )
            appendFieldSection(
                stepHint = steps.getOrNull(1),
                heading = "Body and needs",
                dataId = "bodyAndNeeds",
                text = entry.bodyAndNeeds,
                audioPath = entry.bodyAndNeedsAudioPath,
            )
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Center of Gravity — $dateLabel",
            html = wrapHtml(
                title = "Center of Gravity — $dateLabel",
                createdAtMillis = entry.createdAt,
                body = body,
            ),
        )
    }

    fun renderThoughtDump(entry: ThoughtDumpEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.ThoughtDump)
        val dateLabel = formatDate(entry.createdAt)
        val body = buildString {
            appendLine("<h1>${escapeHtml(tool?.title ?: "Capture Thought")}</h1>")
            appendLine("<p><strong>Mood:</strong> ${entry.moodLevel.coerceIn(1, 5)}/5</p>")
            appendParagraph(entry.content)
            appendAudioNote(entry.audioPath)
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Thought Dump — $dateLabel",
            html = wrapHtml(title = "Thought Dump — $dateLabel", createdAtMillis = entry.createdAt, body = body),
        )
    }

    fun renderAnxietyLog(entry: ThoughtDumpEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.AnxietyLog)
        val dateLabel = formatDate(entry.createdAt)
        val body = buildString {
            appendLine("<h1>${escapeHtml(tool?.title ?: "Anxiety Log")}</h1>")
            appendParagraph(entry.content)
            appendAudioNote(entry.audioPath)
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Anxiety Log — $dateLabel",
            html = wrapHtml(title = "Anxiety Log — $dateLabel", createdAtMillis = entry.createdAt, body = body),
        )
    }

    fun renderFutureSelf(entry: FutureSelfMessageEntity): OneNotePageContent {
        val tool = ToolkitCatalog.byId(ToolkitToolId.FutureSelfMessage)
        val deliverLabel = formatDate(entry.scheduledAtMillis)
        val createdLabel = formatDate(entry.createdAtMillis)
        val body = buildString {
            appendLine("<h1>${escapeHtml(tool?.title ?: "Future Self Message")}</h1>")
            appendLine("<p><strong>Deliver on:</strong> ${escapeHtml(deliverLabel)}</p>")
            appendLine("<p><strong>Written on:</strong> ${escapeHtml(createdLabel)}</p>")
            appendParagraph(entry.content)
            appendAudioNote(entry.audioPath)
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Future Self — $deliverLabel",
            html = wrapHtml(
                title = "Future Self — $deliverLabel",
                createdAtMillis = entry.createdAtMillis,
                body = body,
            ),
        )
    }

    fun renderMeditationReflection(entry: MeditationReflectionEntity): OneNotePageContent {
        val dateLabel = formatDate(entry.completedAt)
        val body = buildString {
            appendLine("<h1>Meditation Reflection</h1>")
            appendLine("<p><strong>Duration:</strong> ${entry.durationSeconds / 60} min</p>")
            appendParagraph(entry.reflection)
            appendFooter(entry.id)
        }
        return OneNotePageContent(
            title = "Meditation Reflection — $dateLabel",
            html = wrapHtml(
                title = "Meditation Reflection — $dateLabel",
                createdAtMillis = entry.completedAt,
                body = body,
            ),
        )
    }

    fun render(entryType: OneNoteEntryType, payload: Any): OneNotePageContent = when (entryType) {
        OneNoteEntryType.NVC -> renderNvc(payload as NvcEntryEntity)
        OneNoteEntryType.REFACTORING -> renderRefactoring(payload as RefactoringEntryEntity)
        OneNoteEntryType.CENTER_OF_GRAVITY -> renderCenterOfGravity(payload as CenterOfGravityEntryEntity)
        OneNoteEntryType.THOUGHT_DUMP -> renderThoughtDump(payload as ThoughtDumpEntity)
        OneNoteEntryType.ANXIETY_LOG -> renderAnxietyLog(payload as ThoughtDumpEntity)
        OneNoteEntryType.FUTURE_SELF -> renderFutureSelf(payload as FutureSelfMessageEntity)
        OneNoteEntryType.MEDITATION_REFLECTION -> renderMeditationReflection(payload as MeditationReflectionEntity)
    }

    internal fun escapeHtml(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private fun StringBuilder.appendFieldSection(
        stepHint: String? = null,
        heading: String,
        dataId: String,
        text: String,
        audioPath: String?,
    ) {
        stepHint?.takeIf { it.isNotBlank() }?.let { hint ->
            appendLine("<p><i>${escapeHtml(hint)}</i></p>")
        }
        appendLine("<h2 data-id=\"$dataId\">${escapeHtml(heading)}</h2>")
        appendParagraph(text)
        appendAudioNote(audioPath)
    }

    private fun StringBuilder.appendParagraph(text: String) {
        if (text.isBlank()) {
            appendLine("<p><i>(empty)</i></p>")
        } else {
            appendLine("<p>${escapeHtml(text).replace("\n", "<br/>")}</p>")
        }
    }

    private fun StringBuilder.appendAudioNote(audioPath: String?) {
        if (!audioPath.isNullOrBlank()) {
            appendLine("<p><i>Audio recorded in app (not synced)</i></p>")
        }
    }

    private fun StringBuilder.appendFooter(localEntryId: Long) {
        appendLine(
            "<p><small>Synced from Serene Interval · Entry #$localEntryId</small></p>",
        )
    }

    private fun wrapHtml(title: String, createdAtMillis: Long, body: String): String {
        val createdMeta = isoFormatter.format(
            Instant.ofEpochMilli(createdAtMillis).atZone(ZoneId.systemDefault()),
        )
        return """
            <!DOCTYPE html>
            <html>
              <head>
                <title>${escapeHtml(title)}</title>
                <meta name="created" content="$createdMeta" />
              </head>
              <body>
                $body
              </body>
            </html>
        """.trimIndent()
    }

    private fun formatDate(epochMillis: Long): String =
        displayDateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

data class OneNotePageContent(
    val title: String,
    val html: String,
)
