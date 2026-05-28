package com.example.meditationparticles.data.export

import android.content.Context
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.TimerPreferences
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerSoundOption
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.reminder.FutureSelfMessageScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Restores settings and journal entries from an export JSON file.
 *
 * Room entries are inserted with new auto-generated IDs. Re-importing the same backup skips
 * rows that match an existing entry by natural key (primary text + createdAt timestamp).
 */
class AppDataImporter(
    private val context: Context,
) {
    suspend fun importFromJson(json: String): ImportResult = withContext(Dispatchers.IO) {
        val root = parseExportRoot(json)
        val warnings = mutableListOf<String>()
        val skips = mutableListOf<ImportSkip>()
        var counts = ImportCounts()

        validateExportVersion(root, warnings)

        val configuration = root.optJSONObject("configuration")
        if (configuration != null) {
            counts = importConfiguration(configuration, counts, skips, warnings)
        } else {
            warnings += "No configuration section found; settings were not changed."
        }

        val entries = root.optJSONObject("entries")
        if (entries != null) {
            counts = importEntries(entries, counts, skips, warnings)
        } else {
            warnings += "No entries section found; journal data was not changed."
        }

        ImportResult(counts = counts, skips = skips, warnings = warnings)
    }

    private fun parseExportRoot(json: String): JSONObject = parseExportDocument(json)

    private fun validateExportVersion(root: JSONObject, warnings: MutableList<String>) {
        if (!root.has("exportVersion")) {
            warnings += "Backup has no version field; importing compatible fields only."
            return
        }
        val version = root.optInt("exportVersion", AppDataExporter.EXPORT_VERSION)
        when {
            version > AppDataExporter.EXPORT_VERSION -> {
                warnings += "Backup version $version is newer than this app supports " +
                    "(${AppDataExporter.EXPORT_VERSION}); some fields may not import."
            }
            version < AppDataExporter.EXPORT_VERSION -> {
                warnings += "Backup version $version is older; compatible fields were imported."
            }
        }
    }

    private fun importConfiguration(
        configuration: JSONObject,
        counts: ImportCounts,
        skips: MutableList<ImportSkip>,
        warnings: MutableList<String>,
    ): ImportCounts {
        var updated = counts

        configuration.optJSONObject("experienceSettings")?.let { experience ->
            runCatching { importExperienceSettings(experience) }
                .onSuccess {
                    AppGraph.settings(context).save(it)
                    updated = updated.copy(experienceSettings = 1)
                }
                .onFailure { error ->
                    skips += ImportSkip(
                        category = "experience settings",
                        reason = "invalid values",
                        detail = error.message,
                    )
                }
        }

        configuration.optJSONObject("toolkitPreferences")?.let { toolkit ->
            runCatching { importToolkitPreferences(toolkit) }
                .onSuccess {
                    applyToolkitPreferences(it)
                    updated = updated.copy(toolkitPreferences = 1)
                }
                .onFailure { error ->
                    skips += ImportSkip(
                        category = "toolkit preferences",
                        reason = "invalid values",
                        detail = error.message,
                    )
                }
        }

        configuration.optJSONObject("affirmationPreferences")?.let { affirmationPrefs ->
            runCatching { importAffirmationPreferences(affirmationPrefs) }
                .onSuccess {
                    AffirmationPreferences(context).save(it)
                    updated = updated.copy(affirmationPreferences = 1)
                }
                .onFailure { error ->
                    skips += ImportSkip(
                        category = "affirmation preferences",
                        reason = "invalid values",
                        detail = error.message,
                    )
                }
        }

        configuration.optJSONObject("timerPreferences")?.let { timerPrefs ->
            runCatching { importTimerPreferences(timerPrefs, warnings) }
                .onSuccess { (snapshot, timerSkips) ->
                    TimerPreferences(context).save(snapshot)
                    skips += timerSkips
                    updated = updated.copy(timerPreferences = 1)
                }
                .onFailure { error ->
                    skips += ImportSkip(
                        category = "timer preferences",
                        reason = "invalid values",
                        detail = error.message,
                    )
                }
        }

        return updated
    }

    private fun importExperienceSettings(json: JSONObject): ExperienceSettings {
        val current = AppGraph.settings(context).load()
        val themeMode = json.optString("themeMode", current.themeMode.name)
            .let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrDefault(current.themeMode)
            }
        val enabledScenes = json.optJSONArray("enabledScenes")?.toStringSet()
            ?: current.enabledScenes

        return current.copy(
            themeMode = themeMode,
            preferredName = json.optString("preferredName", current.preferredName),
            sanctuaryName = json.optString("sanctuaryName", current.sanctuaryName),
            onboardingCompleted = json.optBoolean("onboardingCompleted", current.onboardingCompleted),
            enableBreathing = json.optBoolean("enableBreathing", current.enableBreathing),
            enableTimer = json.optBoolean("enableTimer", current.enableTimer),
            enableAffirmations = json.optBoolean("enableAffirmations", current.enableAffirmations),
            enableToolkit = json.optBoolean("enableToolkit", current.enableToolkit),
            enableVisuals = json.optBoolean("enableVisuals", current.enableVisuals),
            enabledScenes = enabledScenes.ifEmpty { ExperienceSettings.defaultScenes },
            meditationRemindersAvailable = json.optBoolean(
                "meditationRemindersAvailable",
                current.meditationRemindersAvailable,
            ),
            futureSelfSchedulingAvailable = json.optBoolean(
                "futureSelfSchedulingAvailable",
                current.futureSelfSchedulingAvailable,
            ),
        )
    }

    private fun importToolkitPreferences(json: JSONObject): ToolkitImportSnapshot {
        val current = AppGraph.toolkit(context).load(
            AppGraph.settings(context).load().onboardingCompleted,
        )
        val importedEnabled = json.optJSONArray("enabledToolIds")?.toEnumSet<ToolkitToolId>()
            ?: emptySet()
        val mergedEnabled = if (importedEnabled.isEmpty()) {
            current.enabledToolIds
        } else {
            current.enabledToolIds + importedEnabled
        }

        val proactiveOrder = json.optJSONArray("proactiveOrder")?.toEnumList<ToolkitToolId>()
            ?.let { ToolkitLayout.normalizeOrder(ToolkitCategory.Proactive, it) }
            ?: current.proactiveOrder
        val reactiveOrder = json.optJSONArray("reactiveOrder")?.toEnumList<ToolkitToolId>()
            ?.let { ToolkitLayout.normalizeOrder(ToolkitCategory.Reactive, it) }
            ?: current.reactiveOrder
        val usageCounts = json.optJSONObject("usageCounts")?.toUsageCounts()
            ?: current.usageCounts

        return ToolkitImportSnapshot(
            configured = json.optBoolean("configured", current.configured || importedEnabled.isNotEmpty()),
            enabledToolIds = mergedEnabled.ifEmpty { ToolkitLayout.defaultEnabledTools() },
            proactiveOrder = proactiveOrder,
            reactiveOrder = reactiveOrder,
            usageCounts = usageCounts,
        )
    }

    private fun applyToolkitPreferences(snapshot: ToolkitImportSnapshot) {
        val toolkit = AppGraph.toolkit(context)
        val onboardingCompleted = AppGraph.settings(context).load().onboardingCompleted
        if (snapshot.configured) {
            toolkit.saveConfiguration(snapshot.enabledToolIds)
        } else {
            toolkit.setEnabledTools(snapshot.enabledToolIds)
        }
        toolkit.saveProactiveOrder(snapshot.proactiveOrder)
        toolkit.saveReactiveOrder(snapshot.reactiveOrder)
        toolkit.saveUsageCounts(snapshot.usageCounts)
        toolkit.refresh(onboardingCompleted)
    }

    private fun importAffirmationPreferences(json: JSONObject): AffirmationPreferences.AffirmationPrefsSnapshot {
        val current = AffirmationPreferences(context).load()
        return AffirmationPreferences.AffirmationPrefsSnapshot(
            reminderEnabled = json.optBoolean("reminderEnabled", current.reminderEnabled),
            reminderHour = json.optInt("reminderHour", current.reminderHour),
            reminderMinute = json.optInt("reminderMinute", current.reminderMinute),
            viewMode = json.optString("viewMode", current.viewMode),
        )
    }

    private fun importTimerPreferences(
        json: JSONObject,
        warnings: MutableList<String>,
    ): Pair<TimerPreferences.TimerPrefsSnapshot, List<ImportSkip>> {
        val current = TimerPreferences(context).load()
        val skips = mutableListOf<ImportSkip>()
        val displayMode = json.optString("displayMode", current.displayMode.name)
            .let { name ->
                runCatching { TimerDisplayMode.valueOf(name) }.getOrDefault(current.displayMode)
            }
        val sound = TimerSoundOption.fromStoredName(json.optString("sound", current.sound.name))
        val bellSound = TimerBellSoundChoice.fromStoredName(
            json.optString("bellSound", current.bellSound.name),
        )
        val bellSystemUri = json.optString("bellSystemUri").takeIf { it.isNotBlank() }

        return TimerPreferences.TimerPrefsSnapshot(
            displayMode = displayMode,
            targetMinutes = json.optInt("targetMinutes", current.targetMinutes),
            sound = sound,
            bellSound = bellSound,
            bellSystemUri = bellSystemUri,
            reminderEnabled = json.optBoolean("reminderEnabled", current.reminderEnabled),
            reminderHour = json.optInt("reminderHour", current.reminderHour),
            reminderMinute = json.optInt("reminderMinute", current.reminderMinute),
        ) to skips
    }

    private suspend fun importEntries(
        entries: JSONObject,
        counts: ImportCounts,
        skips: MutableList<ImportSkip>,
        warnings: MutableList<String>,
    ): ImportCounts {
        val db = SereneDatabase.getInstance(context)
        var updated = counts

        updated = updated.copy(
            affirmations = importAffirmations(
                array = entries.optJSONArray("affirmations"),
                dao = db.affirmationDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            thoughtDumps = importThoughtDumps(
                array = entries.optJSONArray("thoughtDumps"),
                logType = ToolkitLogType.THOUGHT_DUMP,
                dao = db.thoughtDumpDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            anxietyLogs = importThoughtDumps(
                array = entries.optJSONArray("anxietyLogs"),
                logType = ToolkitLogType.ANXIETY_LOG,
                dao = db.thoughtDumpDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            futureSelfMessages = importFutureSelfMessages(
                array = entries.optJSONArray("futureSelfMessages"),
                dao = db.futureSelfMessageDao(),
                skips = skips,
                warnings = warnings,
            ),
        )
        updated = updated.copy(
            refactoringEntries = importRefactoringEntries(
                array = entries.optJSONArray("refactoringEntries"),
                dao = db.refactoringEntryDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            centerOfGravityEntries = importCenterOfGravityEntries(
                array = entries.optJSONArray("centerOfGravityEntries"),
                dao = db.centerOfGravityEntryDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            nvcEntries = importNvcEntries(
                array = entries.optJSONArray("nvcEntries"),
                dao = db.nvcEntryDao(),
                skips = skips,
            ),
        )

        return updated
    }

    private suspend fun importAffirmations(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.AffirmationDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val text = item.optString("text", "").trim()
            if (text.isEmpty()) {
                skips += ImportSkip("affirmation", "missing text")
                continue
            }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            if (existing.any { it.text == text && it.createdAt == createdAt }) {
                skips += ImportSkip("affirmation", "duplicate", detail = text.take(40))
                continue
            }

            dao.insert(
                AffirmationEntity(
                    text = text,
                    createdAt = createdAt,
                    sortOrder = item.optInt("sortOrder", 0),
                    isFavorite = item.optBoolean("isFavorite", false),
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importThoughtDumps(
        array: JSONArray?,
        logType: ToolkitLogType,
        dao: com.example.meditationparticles.data.local.ThoughtDumpDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll().filter { it.logType == logType.name }
        val categoryLabel = when (logType) {
            ToolkitLogType.THOUGHT_DUMP -> "thought dump"
            ToolkitLogType.ANXIETY_LOG -> "anxiety log"
        }
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val content = item.optString("content", "").trim()
            if (content.isEmpty()) {
                skips += ImportSkip(categoryLabel, "missing content")
                continue
            }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            if (existing.any { it.content == content && it.createdAt == createdAt }) {
                skips += ImportSkip(categoryLabel, "duplicate", detail = content.take(40))
                continue
            }

            val (audioPath, audioSkips) = resolveAudioPath(
                exportedPath = item.optionalString("audioPath"),
                category = categoryLabel,
            )
            skips += audioSkips

            val moodLevel = item.optInt("moodLevel", 3).coerceIn(1, 5)
            dao.insert(
                ThoughtDumpEntity(
                    content = content,
                    logType = logType.name,
                    moodLevel = moodLevel,
                    audioPath = audioPath,
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importFutureSelfMessages(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.FutureSelfMessageDao,
        skips: MutableList<ImportSkip>,
        warnings: MutableList<String>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        val now = System.currentTimeMillis()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val content = item.optString("content", "").trim()
            if (content.isEmpty()) {
                skips += ImportSkip("future self message", "missing content")
                continue
            }

            val scheduledAtMillis = item.optLong("scheduledAtMillis", -1L)
            if (scheduledAtMillis <= 0L) {
                skips += ImportSkip("future self message", "invalid schedule date")
                continue
            }

            val createdAtMillis = item.optLong("createdAtMillis", scheduledAtMillis)
            if (existing.any { it.content == content && it.scheduledAtMillis == scheduledAtMillis }) {
                skips += ImportSkip("future self message", "duplicate", detail = content.take(40))
                continue
            }

            val delivered = item.optBoolean("delivered", false)
            val (audioPath, audioSkips) = resolveAudioPath(
                exportedPath = item.optionalString("audioPath"),
                category = "future self message",
            )
            skips += audioSkips

            val newId = dao.insert(
                FutureSelfMessageEntity(
                    content = content,
                    audioPath = audioPath,
                    scheduledAtMillis = scheduledAtMillis,
                    createdAtMillis = createdAtMillis,
                    delivered = delivered,
                ),
            )

            if (!delivered && scheduledAtMillis > now) {
                val scheduled = FutureSelfMessageScheduler.schedule(
                    context = context,
                    messageId = newId,
                    triggerAtMillis = scheduledAtMillis,
                )
                if (!scheduled) {
                    warnings += "Could not schedule alarm for a future self message."
                }
            }

            imported++
        }
        return imported
    }

    private suspend fun importRefactoringEntries(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.RefactoringEntryDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val interpretation = item.optString("interpretation", "").trim()
            val actualFacts = item.optString("actualFacts", "").trim()
            if (interpretation.isEmpty() && actualFacts.isEmpty()) {
                skips += ImportSkip("refactoring entry", "missing content")
                continue
            }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            if (existing.any {
                    it.interpretation == interpretation &&
                        it.actualFacts == actualFacts &&
                        it.createdAt == createdAt
                }
            ) {
                skips += ImportSkip("refactoring entry", "duplicate")
                continue
            }

            val audioSkips = mutableListOf<ImportSkip>()
            val interpretationAudio = resolveAudioPath(
                item.optionalString("interpretationAudioPath"),
                "refactoring entry",
            )
            val actualFactsAudio = resolveAudioPath(
                item.optionalString("actualFactsAudioPath"),
                "refactoring entry",
            )
            val explanation1Audio = resolveAudioPath(
                item.optionalString("explanation1AudioPath"),
                "refactoring entry",
            )
            val explanation2Audio = resolveAudioPath(
                item.optionalString("explanation2AudioPath"),
                "refactoring entry",
            )
            val explanation3Audio = resolveAudioPath(
                item.optionalString("explanation3AudioPath"),
                "refactoring entry",
            )
            audioSkips += interpretationAudio.second + actualFactsAudio.second +
                explanation1Audio.second + explanation2Audio.second + explanation3Audio.second
            skips += audioSkips

            dao.insert(
                RefactoringEntryEntity(
                    interpretation = interpretation,
                    interpretationAudioPath = interpretationAudio.first,
                    actualFacts = actualFacts,
                    actualFactsAudioPath = actualFactsAudio.first,
                    explanation1 = item.optString("explanation1", ""),
                    explanation1AudioPath = explanation1Audio.first,
                    explanation2 = item.optString("explanation2", ""),
                    explanation2AudioPath = explanation2Audio.first,
                    explanation3 = item.optString("explanation3", ""),
                    explanation3AudioPath = explanation3Audio.first,
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importCenterOfGravityEntries(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.CenterOfGravityEntryDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val thoughtsAndFeelings = item.optString("thoughtsAndFeelings", "").trim()
            val bodyAndNeeds = item.optString("bodyAndNeeds", "").trim()
            if (thoughtsAndFeelings.isEmpty() && bodyAndNeeds.isEmpty()) {
                skips += ImportSkip("center of gravity entry", "missing content")
                continue
            }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            if (existing.any {
                    it.thoughtsAndFeelings == thoughtsAndFeelings &&
                        it.bodyAndNeeds == bodyAndNeeds &&
                        it.createdAt == createdAt
                }
            ) {
                skips += ImportSkip("center of gravity entry", "duplicate")
                continue
            }

            val thoughtsAudio = resolveAudioPath(
                item.optionalString("thoughtsAndFeelingsAudioPath"),
                "center of gravity entry",
            )
            val bodyAudio = resolveAudioPath(
                item.optionalString("bodyAndNeedsAudioPath"),
                "center of gravity entry",
            )
            skips += thoughtsAudio.second + bodyAudio.second

            dao.insert(
                CenterOfGravityEntryEntity(
                    thoughtsAndFeelings = thoughtsAndFeelings,
                    thoughtsAndFeelingsAudioPath = thoughtsAudio.first,
                    bodyAndNeeds = bodyAndNeeds,
                    bodyAndNeedsAudioPath = bodyAudio.first,
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importNvcEntries(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.NvcEntryDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val observation = item.optString("observation", "").trim()
            val feeling = item.optString("feeling", "").trim()
            val need = item.optString("need", "").trim()
            val request = item.optString("request", "").trim()
            if (observation.isEmpty() && feeling.isEmpty() && need.isEmpty() && request.isEmpty()) {
                skips += ImportSkip("NVC entry", "missing content")
                continue
            }
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())
            if (existing.any {
                    it.observation == observation &&
                        it.feeling == feeling &&
                        it.need == need &&
                        it.request == request &&
                        it.createdAt == createdAt
                }
            ) {
                skips += ImportSkip("NVC entry", "duplicate")
                continue
            }

            val observationAudio = resolveAudioPath(
                item.optionalString("observationAudioPath"),
                "NVC entry",
            )
            val feelingAudio = resolveAudioPath(
                item.optionalString("feelingAudioPath"),
                "NVC entry",
            )
            val needAudio = resolveAudioPath(
                item.optionalString("needAudioPath"),
                "NVC entry",
            )
            val requestAudio = resolveAudioPath(
                item.optionalString("requestAudioPath"),
                "NVC entry",
            )
            skips += observationAudio.second + feelingAudio.second + needAudio.second + requestAudio.second

            dao.insert(
                NvcEntryEntity(
                    observation = observation,
                    observationAudioPath = observationAudio.first,
                    feeling = feeling,
                    feelingAudioPath = feelingAudio.first,
                    need = need,
                    needAudioPath = needAudio.first,
                    request = request,
                    requestAudioPath = requestAudio.first,
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private fun resolveAudioPath(
        exportedPath: String?,
        category: String,
    ): Pair<String?, List<ImportSkip>> {
        if (exportedPath.isNullOrBlank()) return null to emptyList()
        val file = File(exportedPath)
        return if (file.exists()) {
            exportedPath to emptyList()
        } else {
            null to listOf(
                ImportSkip(
                    category = category,
                    reason = "audio file missing (text imported)",
                    detail = exportedPath,
                ),
            )
        }
    }

    private data class ToolkitImportSnapshot(
        val configured: Boolean,
        val enabledToolIds: Set<ToolkitToolId>,
        val proactiveOrder: List<ToolkitToolId>,
        val reactiveOrder: List<ToolkitToolId>,
        val usageCounts: Map<ToolkitToolId, Int>,
    )

    companion object {
        fun validateExportJson(json: String): Int {
            val root = parseExportDocument(json)
            return root.optInt("exportVersion", AppDataExporter.EXPORT_VERSION)
        }

        fun parseExportDocument(json: String): JSONObject {
            val trimmed = json.trim()
            if (trimmed.isEmpty()) {
                throw ImportParseException("The selected file is empty.")
            }
            return try {
                JSONObject(trimmed)
            } catch (error: Exception) {
                throw ImportParseException(
                    "Could not read backup file. Make sure it is valid JSON exported from this app.",
                    error,
                )
            }
        }
    }
}

private fun JSONObject.optionalString(key: String): String? =
    if (has(key) && !isNull(key)) getString(key).takeIf { it.isNotBlank() } else null

private inline fun <reified T : Enum<T>> JSONArray.toEnumSet(): Set<T> = buildSet {
    for (index in 0 until length()) {
        val name = optString(index, "").trim()
        if (name.isEmpty()) continue
        runCatching { add(enumValueOf<T>(name)) }
    }
}

private inline fun <reified T : Enum<T>> JSONArray.toEnumList(): List<T> = buildList {
    for (index in 0 until length()) {
        val name = optString(index, "").trim()
        if (name.isEmpty()) continue
        runCatching { add(enumValueOf<T>(name)) }
    }
}

private fun JSONArray.toStringSet(): Set<String> = buildSet {
    for (index in 0 until length()) {
        val value = optString(index, "").trim()
        if (value.isNotEmpty()) add(value)
    }
}

private fun JSONObject.toUsageCounts(): Map<ToolkitToolId, Int> = buildMap {
    keys().forEach { key ->
        val id = runCatching { ToolkitToolId.valueOf(key) }.getOrNull() ?: return@forEach
        val count = optInt(key, 0).coerceAtLeast(0)
        if (count > 0) put(id, count)
    }
}
