package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.data.local.SereneDatabase

object AppGraph {
    @Volatile
    private var affirmationRepository: AffirmationRepository? = null
    @Volatile
    private var thoughtDumpRepository: ThoughtDumpRepository? = null
    @Volatile
    private var settingsPreferences: SettingsPreferences? = null
    @Volatile
    private var toolkitPreferences: ToolkitPreferences? = null
    @Volatile
    private var sessionRepository: SessionRepository? = null

    @Volatile
    private var futureSelfMessageRepository: FutureSelfMessageRepository? = null

    fun affirmations(context: Context): AffirmationRepository =
        affirmationRepository ?: synchronized(this) {
            affirmationRepository ?: AffirmationRepository(
                SereneDatabase.getInstance(context.applicationContext).affirmationDao(),
            ).also { affirmationRepository = it }
        }

    fun thoughtDumps(context: Context): ThoughtDumpRepository =
        thoughtDumpRepository ?: synchronized(this) {
            thoughtDumpRepository ?: ThoughtDumpRepository(
                SereneDatabase.getInstance(context.applicationContext).thoughtDumpDao(),
            ).also { thoughtDumpRepository = it }
        }

    fun settings(context: Context): SettingsPreferences =
        settingsPreferences ?: synchronized(this) {
            settingsPreferences ?: SettingsPreferences(context.applicationContext)
                .also { settingsPreferences = it }
        }

    fun toolkit(context: Context): ToolkitPreferences =
        toolkitPreferences ?: synchronized(this) {
            toolkitPreferences ?: ToolkitPreferences(context.applicationContext)
                .also { toolkitPreferences = it }
        }

    fun sessions(context: Context): SessionRepository =
        sessionRepository ?: synchronized(this) {
            sessionRepository ?: SessionRepository(
                SereneDatabase.getInstance(context.applicationContext).sessionDao(),
            ).also { sessionRepository = it }
        }

    fun futureSelfMessages(context: Context): FutureSelfMessageRepository =
        futureSelfMessageRepository ?: synchronized(this) {
            futureSelfMessageRepository ?: FutureSelfMessageRepository(
                SereneDatabase.getInstance(context.applicationContext).futureSelfMessageDao(),
            ).also { futureSelfMessageRepository = it }
        }
}
