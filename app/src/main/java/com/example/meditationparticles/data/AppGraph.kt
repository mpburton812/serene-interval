package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.data.onenote.OneNoteAuthManager
import com.example.meditationparticles.data.onenote.OneNoteGraphClient
import com.example.meditationparticles.data.onenote.OneNotePreferences
import com.example.meditationparticles.data.onenote.OneNoteSyncRepository

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
    private var quickStartPreferences: QuickStartPreferences? = null
    @Volatile
    private var sessionRepository: SessionRepository? = null

    @Volatile
    private var meditationReflectionRepository: MeditationReflectionRepository? = null

    @Volatile
    private var futureSelfMessageRepository: FutureSelfMessageRepository? = null

    @Volatile
    private var refactoringRepository: RefactoringRepository? = null

    @Volatile
    private var centerOfGravityRepository: CenterOfGravityRepository? = null

    @Volatile
    private var nvcRepository: NvcRepository? = null

    @Volatile
    private var tabBackgroundRotation: TabBackgroundRotation? = null

    @Volatile
    private var oneNotePreferences: OneNotePreferences? = null

    @Volatile
    private var oneNoteAuthManager: OneNoteAuthManager? = null

    @Volatile
    private var oneNoteSyncRepository: OneNoteSyncRepository? = null

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

    fun quickStart(context: Context): QuickStartPreferences =
        quickStartPreferences ?: synchronized(this) {
            quickStartPreferences ?: QuickStartPreferences(context.applicationContext)
                .also { quickStartPreferences = it }
        }

    fun sessions(context: Context): SessionRepository =
        sessionRepository ?: synchronized(this) {
            sessionRepository ?: SessionRepository(
                SereneDatabase.getInstance(context.applicationContext).sessionDao(),
            ).also { sessionRepository = it }
        }

    fun meditationReflections(context: Context): MeditationReflectionRepository =
        meditationReflectionRepository ?: synchronized(this) {
            meditationReflectionRepository ?: MeditationReflectionRepository(
                SereneDatabase.getInstance(context.applicationContext).meditationReflectionDao(),
            ).also { meditationReflectionRepository = it }
        }

    fun futureSelfMessages(context: Context): FutureSelfMessageRepository =
        futureSelfMessageRepository ?: synchronized(this) {
            futureSelfMessageRepository ?: FutureSelfMessageRepository(
                SereneDatabase.getInstance(context.applicationContext).futureSelfMessageDao(),
            ).also { futureSelfMessageRepository = it }
        }

    fun refactoringEntries(context: Context): RefactoringRepository =
        refactoringRepository ?: synchronized(this) {
            refactoringRepository ?: RefactoringRepository(
                SereneDatabase.getInstance(context.applicationContext).refactoringEntryDao(),
            ).also { refactoringRepository = it }
        }

    fun centerOfGravityEntries(context: Context): CenterOfGravityRepository =
        centerOfGravityRepository ?: synchronized(this) {
            centerOfGravityRepository ?: CenterOfGravityRepository(
                SereneDatabase.getInstance(context.applicationContext).centerOfGravityEntryDao(),
            ).also { centerOfGravityRepository = it }
        }

    fun nvcEntries(context: Context): NvcRepository =
        nvcRepository ?: synchronized(this) {
            nvcRepository ?: NvcRepository(
                SereneDatabase.getInstance(context.applicationContext).nvcEntryDao(),
            ).also { nvcRepository = it }
        }

    fun tabBackgroundRotation(context: Context): TabBackgroundRotation =
        tabBackgroundRotation ?: synchronized(this) {
            tabBackgroundRotation ?: TabBackgroundRotation(context.applicationContext)
                .also { tabBackgroundRotation = it }
        }

    fun oneNotePreferences(context: Context): OneNotePreferences =
        oneNotePreferences ?: synchronized(this) {
            oneNotePreferences ?: OneNotePreferences(context.applicationContext)
                .also { oneNotePreferences = it }
        }

    fun oneNoteAuth(context: Context): OneNoteAuthManager =
        oneNoteAuthManager ?: synchronized(this) {
            oneNoteAuthManager ?: OneNoteAuthManager(context.applicationContext)
                .also { oneNoteAuthManager = it }
        }

    fun oneNoteSync(context: Context): OneNoteSyncRepository =
        oneNoteSyncRepository ?: synchronized(this) {
            oneNoteSyncRepository ?: OneNoteSyncRepository(
                context = context.applicationContext,
                preferences = oneNotePreferences(context),
                authManager = oneNoteAuth(context),
                graphClient = OneNoteGraphClient(),
                syncDao = SereneDatabase.getInstance(context.applicationContext).oneNoteSyncDao(),
                database = SereneDatabase.getInstance(context.applicationContext),
            ).also { oneNoteSyncRepository = it }
        }
}
