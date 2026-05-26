package com.example.meditationparticles.data.export

import android.content.Context
import com.example.meditationparticles.BuildConfig
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
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class AppDataExporter(
    private val context: Context,
) {
    suspend fun buildExportJson(): String = withContext(Dispatchers.IO) {
        val db = SereneDatabase.getInstance(context)
        val settings = AppGraph.settings(context).load()
        val toolkit = AppGraph.toolkit(context).load(settings.onboardingCompleted)
        val affirmationPrefs = AffirmationPreferences(context).load()
        val timerPrefs = TimerPreferences(context).load()

        val thoughtDumps = db.thoughtDumpDao().getAll()
        val affirmations = db.affirmationDao().getAll()
        val futureSelfMessages = db.futureSelfMessageDao().getAll()
        val refactoringEntries = db.refactoringEntryDao().getAll()
        val centerOfGravityEntries = db.centerOfGravityEntryDao().getAll()
        val nvcEntries = db.nvcEntryDao().getAll()

        JSONObject().apply {
            put("exportVersion", EXPORT_VERSION)
            put("exportedAt", Instant.now().toString())
            put("appVersionName", BuildConfig.VERSION_NAME)
            put("appVersionCode", BuildConfig.VERSION_CODE)
            put("configuration", buildConfiguration(settings, toolkit, affirmationPrefs, timerPrefs))
            put("entries", buildEntries(
                affirmations = affirmations,
                thoughtDumps = thoughtDumps,
                futureSelfMessages = futureSelfMessages,
                refactoringEntries = refactoringEntries,
                centerOfGravityEntries = centerOfGravityEntries,
                nvcEntries = nvcEntries,
            ))
        }.toString(2)
    }

    private fun buildConfiguration(
        settings: ExperienceSettings,
        toolkit: com.example.meditationparticles.data.ToolkitPrefsSnapshot,
        affirmationPrefs: AffirmationPreferences.AffirmationPrefsSnapshot,
        timerPrefs: TimerPreferences.TimerPrefsSnapshot,
    ): JSONObject = JSONObject().apply {
        put("experienceSettings", JSONObject().apply {
            put("themeMode", settings.themeMode.name)
            put("preferredName", settings.preferredName)
            put("sanctuaryName", settings.sanctuaryName)
            put("onboardingCompleted", settings.onboardingCompleted)
            put("enableBreathing", settings.enableBreathing)
            put("enableTimer", settings.enableTimer)
            put("enableAffirmations", settings.enableAffirmations)
            put("enableToolkit", settings.enableToolkit)
            put("enableVisuals", settings.enableVisuals)
            put("enabledScenes", JSONArray(settings.enabledScenes.toList()))
            put("meditationRemindersAvailable", settings.meditationRemindersAvailable)
            put("futureSelfSchedulingAvailable", settings.futureSelfSchedulingAvailable)
        })
        put("toolkitPreferences", JSONObject().apply {
            put("configured", toolkit.configured)
            put("enabledToolIds", JSONArray(toolkit.enabledToolIds.map { it.name }))
            put("proactiveOrder", JSONArray(toolkit.proactiveOrder.map { it.name }))
            put("reactiveOrder", JSONArray(toolkit.reactiveOrder.map { it.name }))
            put("usageCounts", JSONObject().apply {
                toolkit.usageCounts.forEach { (id, count) ->
                    put(id.name, count)
                }
            })
        })
        put("affirmationPreferences", JSONObject().apply {
            put("reminderEnabled", affirmationPrefs.reminderEnabled)
            put("reminderHour", affirmationPrefs.reminderHour)
            put("reminderMinute", affirmationPrefs.reminderMinute)
            put("viewMode", affirmationPrefs.viewMode)
        })
        put("timerPreferences", JSONObject().apply {
            put("displayMode", timerPrefs.displayMode.name)
            put("targetMinutes", timerPrefs.targetMinutes)
            put("sound", timerPrefs.sound.name)
            put("bellSound", timerPrefs.bellSound.name)
            put("bellSystemUri", timerPrefs.bellSystemUri)
            put("reminderEnabled", timerPrefs.reminderEnabled)
            put("reminderHour", timerPrefs.reminderHour)
            put("reminderMinute", timerPrefs.reminderMinute)
        })
    }

    private fun buildEntries(
        affirmations: List<AffirmationEntity>,
        thoughtDumps: List<ThoughtDumpEntity>,
        futureSelfMessages: List<FutureSelfMessageEntity>,
        refactoringEntries: List<RefactoringEntryEntity>,
        centerOfGravityEntries: List<CenterOfGravityEntryEntity>,
        nvcEntries: List<NvcEntryEntity>,
    ): JSONObject = JSONObject().apply {
        put("affirmations", JSONArray().apply {
            affirmations.forEach { put(it.toJson()) }
        })
        put("thoughtDumps", JSONArray().apply {
            thoughtDumps
                .filter { it.logType == ToolkitLogType.THOUGHT_DUMP.name }
                .forEach { put(it.toJson()) }
        })
        put("anxietyLogs", JSONArray().apply {
            thoughtDumps
                .filter { it.logType == ToolkitLogType.ANXIETY_LOG.name }
                .forEach { put(it.toJson()) }
        })
        put("futureSelfMessages", JSONArray().apply {
            futureSelfMessages.forEach { put(it.toJson()) }
        })
        put("refactoringEntries", JSONArray().apply {
            refactoringEntries.forEach { put(it.toJson()) }
        })
        put("centerOfGravityEntries", JSONArray().apply {
            centerOfGravityEntries.forEach { put(it.toJson()) }
        })
        put("nvcEntries", JSONArray().apply {
            nvcEntries.forEach { put(it.toJson()) }
        })
    }

    private fun AffirmationEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("text", text)
        put("createdAt", createdAt)
        put("sortOrder", sortOrder)
        put("isFavorite", isFavorite)
    }

    private fun ThoughtDumpEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("content", content)
        put("logType", logType)
        put("audioPath", audioPath)
        put("createdAt", createdAt)
    }

    private fun FutureSelfMessageEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("content", content)
        put("audioPath", audioPath)
        put("scheduledAtMillis", scheduledAtMillis)
        put("createdAtMillis", createdAtMillis)
        put("delivered", delivered)
    }

    private fun RefactoringEntryEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("interpretation", interpretation)
        put("interpretationAudioPath", interpretationAudioPath)
        put("actualFacts", actualFacts)
        put("actualFactsAudioPath", actualFactsAudioPath)
        put("explanation1", explanation1)
        put("explanation1AudioPath", explanation1AudioPath)
        put("explanation2", explanation2)
        put("explanation2AudioPath", explanation2AudioPath)
        put("explanation3", explanation3)
        put("explanation3AudioPath", explanation3AudioPath)
        put("createdAt", createdAt)
    }

    private fun CenterOfGravityEntryEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("thoughtsAndFeelings", thoughtsAndFeelings)
        put("thoughtsAndFeelingsAudioPath", thoughtsAndFeelingsAudioPath)
        put("bodyAndNeeds", bodyAndNeeds)
        put("bodyAndNeedsAudioPath", bodyAndNeedsAudioPath)
        put("createdAt", createdAt)
    }

    private fun NvcEntryEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("observation", observation)
        put("observationAudioPath", observationAudioPath)
        put("feeling", feeling)
        put("feelingAudioPath", feelingAudioPath)
        put("need", need)
        put("needAudioPath", needAudioPath)
        put("request", request)
        put("requestAudioPath", requestAudioPath)
        put("createdAt", createdAt)
    }

    companion object {
        const val EXPORT_VERSION = 1
        const val DEFAULT_FILENAME = "serene-interval-export.json"
    }
}
