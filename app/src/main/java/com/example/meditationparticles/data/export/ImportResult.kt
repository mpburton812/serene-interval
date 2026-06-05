package com.example.meditationparticles.data.export

data class ImportSkip(
    val category: String,
    val reason: String,
    val detail: String? = null,
)

data class ImportCounts(
    val experienceSettings: Int = 0,
    val toolkitPreferences: Int = 0,
    val affirmationPreferences: Int = 0,
    val timerPreferences: Int = 0,
    val affirmations: Int = 0,
    val thoughtDumps: Int = 0,
    val anxietyLogs: Int = 0,
    val futureSelfMessages: Int = 0,
    val refactoringEntries: Int = 0,
    val centerOfGravityEntries: Int = 0,
    val nvcEntries: Int = 0,
) {
    val totalEntries: Int
        get() = affirmations + thoughtDumps + anxietyLogs + futureSelfMessages +
            refactoringEntries + centerOfGravityEntries + nvcEntries
}

data class ImportResult(
    val counts: ImportCounts,
    val skips: List<ImportSkip> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    fun buildSummary(): String {
        val imported = buildList {
            if (counts.experienceSettings > 0) add("experience settings")
            if (counts.toolkitPreferences > 0) add("toolkit preferences")
            if (counts.affirmationPreferences > 0) add("affirmation preferences")
            if (counts.timerPreferences > 0) add("timer preferences")
            if (counts.affirmations > 0) {
                add("${counts.affirmations} affirmation${counts.affirmations.pluralSuffix()}")
            }
            if (counts.thoughtDumps > 0) {
                add("${counts.thoughtDumps} thought dump${counts.thoughtDumps.pluralSuffix()}")
            }
            if (counts.anxietyLogs > 0) {
                add("${counts.anxietyLogs} anxiety log${counts.anxietyLogs.pluralSuffix()}")
            }
            if (counts.futureSelfMessages > 0) {
                add(
                    "${counts.futureSelfMessages} future self message" +
                        counts.futureSelfMessages.pluralSuffix(),
                )
            }
            if (counts.refactoringEntries > 0) {
                add(
                    "${counts.refactoringEntries} refactoring entr" +
                        if (counts.refactoringEntries == 1) "y" else "ies",
                )
            }
            if (counts.centerOfGravityEntries > 0) {
                add(
                    "${counts.centerOfGravityEntries} center of gravity entr" +
                        if (counts.centerOfGravityEntries == 1) "y" else "ies",
                )
            }
            if (counts.nvcEntries > 0) {
                add(
                    "${counts.nvcEntries} NVC entr" +
                        if (counts.nvcEntries == 1) "y" else "ies",
                )
            }
        }

        val sections = buildList {
            if (imported.isNotEmpty()) {
                add("Imported: ${imported.joinToString(", ")}.")
            } else if (skips.isEmpty() && warnings.isEmpty()) {
                add("Nothing to import.")
            }
            warnings.forEach { add(it) }
            if (skips.isNotEmpty()) {
                val skipLines = skips.groupBy { it.category to it.reason }
                    .map { (key, items) ->
                        val (category, reason) = key
                        val count = items.size
                        val detail = items.firstNotNullOfOrNull { it.detail }
                        buildString {
                            append("$count $category ($reason)")
                            if (detail != null && count == 1) append(": $detail")
                        }
                    }
                add("Skipped: ${skipLines.joinToString("; ")}.")
            }
        }

        return sections.joinToString("\n\n")
    }

    private fun Int.pluralSuffix(): String = if (this == 1) "" else "s"
}

class ImportParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
