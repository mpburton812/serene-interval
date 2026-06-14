package com.example.meditationparticles.data.export

import android.content.Context
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.TimerPreferences
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.LivingTreePersonEntity
import com.example.meditationparticles.data.local.LivingTreePersonTagCrossRef
import com.example.meditationparticles.data.local.LivingTreeTagEntity
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.MoodSource
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
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

        root.optJSONObject("livingTree")?.let { livingTree ->
            counts = importLivingTree(livingTree, counts, skips)
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

        configuration.optJSONObject("quickStartPreferences")?.let { quickStart ->
            runCatching { importQuickStartPreferences(quickStart) }
                .onSuccess {
                    applyQuickStartPreferences(it)
                }
                .onFailure { error ->
                    skips += ImportSkip(
                        category = "quick start preferences",
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
            enableLivingTree = json.optBoolean("enableLivingTree", current.enableLivingTree),
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

    private fun importQuickStartPreferences(json: JSONObject): List<QuickStartTarget> {
        val settings = AppGraph.settings(context).load()
        val toolkit = AppGraph.toolkit(context).snapshot.value
        val imported = json.optJSONArray("selectedIds")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                QuickStartTarget.decode(array.optString(index))
            }
        } ?: emptyList()
        return QuickStartLayout.normalizeSelection(imported, settings, toolkit.enabledToolIds)
    }

    private fun applyQuickStartPreferences(selection: List<QuickStartTarget>) {
        val settings = AppGraph.settings(context).load()
        AppGraph.quickStart(context).saveSelection(selection, settings)
    }

    private fun importAffirmationPreferences(json: JSONObject): AffirmationPreferences.AffirmationPrefsSnapshot {
        val current = AffirmationPreferences(context).load()
        return AffirmationPreferences.AffirmationPrefsSnapshot(
            reminderEnabled = json.optBoolean("reminderEnabled", current.reminderEnabled),
            reminderHour = json.optInt("reminderHour", current.reminderHour),
            reminderMinute = json.optInt("reminderMinute", current.reminderMinute),
        )
    }

    private fun importTimerPreferences(
        json: JSONObject,
        warnings: MutableList<String>,
    ): Pair<TimerPreferences.TimerPrefsSnapshot, List<ImportSkip>> {
        val current = TimerPreferences(context).load()
        val skips = mutableListOf<ImportSkip>()
        val displayMode = TimerDisplayMode.fromStoredName(
            json.optString("displayMode", current.displayMode.name),
        )
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
        updated = updated.copy(
            meditationReflections = importMeditationReflections(
                array = entries.optJSONArray("meditationReflections"),
                dao = db.meditationReflectionDao(),
                skips = skips,
            ),
        )
        updated = updated.copy(
            moodEntries = importMoodEntries(
                array = entries.optJSONArray("moodEntries"),
                dao = db.moodEntryDao(),
                skips = skips,
            ),
        )

        return updated
    }

    private suspend fun importLivingTree(
        json: JSONObject,
        counts: ImportCounts,
        skips: MutableList<ImportSkip>,
    ): ImportCounts {
        val tagsArray = json.optJSONArray("tags") ?: JSONArray()
        val peopleArray = json.optJSONArray("people") ?: JSONArray()
        val personTagsArray = json.optJSONArray("personTags") ?: JSONArray()

        val tags = buildList {
            for (index in 0 until tagsArray.length()) {
                val item = tagsArray.optJSONObject(index) ?: continue
                val name = item.optString("name", "").trim()
                if (name.isEmpty()) {
                    skips += ImportSkip("living tree tag", "missing name")
                    continue
                }
                add(
                    LivingTreeTagEntity(
                        id = item.optLong("id"),
                        name = name,
                        colorArgb = item.optInt("colorArgb", 0xFF4A654E.toInt()),
                        sortOrder = item.optInt("sortOrder", size),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                    ),
                )
            }
        }

        val people = buildList {
            for (index in 0 until peopleArray.length()) {
                val item = peopleArray.optJSONObject(index) ?: continue
                val name = item.optString("name", "").trim()
                if (name.isEmpty()) {
                    skips += ImportSkip("living tree person", "missing name")
                    continue
                }
                add(
                    LivingTreePersonEntity(
                        id = item.optLong("id"),
                        name = name,
                        notes = item.optString("notes", ""),
                        sortOrder = item.optInt("sortOrder", size),
                        angleRadians = item.optDouble("angleRadians").takeIf { item.has("angleRadians") },
                        radiusFraction = item.optDouble("radiusFraction").takeIf { item.has("radiusFraction") },
                        isUserPlaced = item.optBoolean("isUserPlaced", false),
                        createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                        updatedAtMillis = item.optLong("updatedAtMillis", System.currentTimeMillis()),
                    ),
                )
            }
        }

        val personTags = buildList {
            for (index in 0 until personTagsArray.length()) {
                val item = personTagsArray.optJSONObject(index) ?: continue
                add(
                    LivingTreePersonTagCrossRef(
                        personId = item.optLong("personId"),
                        tagId = item.optLong("tagId"),
                    ),
                )
            }
        }

        runCatching {
            AppGraph.livingTree(context).replaceAllFromExport(tags, people, personTags)
        }.onFailure { error ->
            skips += ImportSkip(
                category = "living tree",
                reason = "import failed",
                detail = error.message,
            )
            return counts
        }

        return counts.copy(
            livingTreeTags = tags.size,
            livingTreePeople = people.size,
        )
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

            val moodLevel = item.optionalMoodLevel()
            dao.insert(
                ThoughtDumpEntity(
                    content = content,
                    logType = logType.name,
                    moodLevel = moodLevel,
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

            val newId = dao.insert(
                FutureSelfMessageEntity(
                    content = content,
                    moodLevel = item.optionalMoodLevel(),
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

            dao.insert(
                RefactoringEntryEntity(
                    interpretation = interpretation,
                    actualFacts = actualFacts,
                    explanation1 = item.optString("explanation1", ""),
                    explanation2 = item.optString("explanation2", ""),
                    explanation3 = item.optString("explanation3", ""),
                    moodLevel = item.optionalMoodLevel(),
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

            dao.insert(
                CenterOfGravityEntryEntity(
                    thoughtsAndFeelings = thoughtsAndFeelings,
                    bodyAndNeeds = bodyAndNeeds,
                    moodLevel = item.optionalMoodLevel(),
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importMeditationReflections(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.MeditationReflectionDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        val existing = dao.getAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val reflection = item.optString("reflection", "").trim()
            if (reflection.isEmpty()) {
                skips += ImportSkip("meditation reflection", "missing content")
                continue
            }
            val completedAt = item.optLong("completedAt", System.currentTimeMillis())
            val durationSeconds = item.optInt("durationSeconds", 0).coerceAtLeast(0)
            if (existing.any { it.reflection == reflection && it.completedAt == completedAt }) {
                skips += ImportSkip("meditation reflection", "duplicate", detail = reflection.take(40))
                continue
            }

            dao.insert(
                MeditationReflectionEntity(
                    reflection = reflection,
                    moodLevel = item.optionalMoodLevel(),
                    durationSeconds = durationSeconds,
                    completedAt = completedAt,
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

            dao.insert(
                NvcEntryEntity(
                    observation = observation,
                    feeling = feeling,
                    need = need,
                    request = request,
                    moodLevel = item.optionalMoodLevel(),
                    createdAt = createdAt,
                ),
            )
            imported++
        }
        return imported
    }

    private suspend fun importMoodEntries(
        array: JSONArray?,
        dao: com.example.meditationparticles.data.local.MoodEntryDao,
        skips: MutableList<ImportSkip>,
    ): Int {
        if (array == null || array.length() == 0) return 0
        dao.clearAll()
        var imported = 0

        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val moodLevel = item.optionalMoodLevel()
            if (moodLevel == null) {
                skips += ImportSkip("mood entry", "invalid mood level")
                continue
            }
            val recordedAtMillis = item.optLong("recordedAtMillis", -1L)
            if (recordedAtMillis <= 0L) {
                skips += ImportSkip("mood entry", "invalid timestamp")
                continue
            }
            val sourceName = item.optString("source", "").trim()
            val source = MoodSource.fromDbValue(sourceName)
            if (source == null) {
                skips += ImportSkip("mood entry", "invalid source", detail = sourceName)
                continue
            }
            val legacyTable = item.optString("legacyTable").takeIf { it.isNotBlank() }
            val legacyRowId = if (item.has("legacyRowId") && !item.isNull("legacyRowId")) {
                item.optLong("legacyRowId")
            } else {
                null
            }

            dao.insert(
                MoodEntryEntity(
                    moodLevel = moodLevel,
                    recordedAtMillis = recordedAtMillis,
                    source = source.dbValue,
                    legacyTable = legacyTable,
                    legacyRowId = legacyRowId,
                ),
            )
            imported++
        }
        return imported
    }

    private fun JSONObject.optionalMoodLevel(): Int? {
        if (!has("moodLevel") || isNull("moodLevel")) return null
        return MoodScale.normalize(optInt("moodLevel"))
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
