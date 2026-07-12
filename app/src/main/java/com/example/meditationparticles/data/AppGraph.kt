package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.data.backup.AutoBackupManager
import com.example.meditationparticles.data.backup.AutoBackupPreferences
import com.example.meditationparticles.data.backup.AutoBackupScheduler
import com.example.meditationparticles.data.local.SereneDatabase
import com.example.meditationparticles.data.onenote.OneNoteAuthManager
import com.example.meditationparticles.data.onenote.OneNoteGraphClient
import com.example.meditationparticles.data.onenote.OneNotePreferences
import com.example.meditationparticles.data.onenote.OneNoteSyncRepository
import com.example.meditationparticles.domain.affirmations.AffirmationListKind

object AppGraph {
    @Volatile
    private var affirmationRepositories: MutableMap<AffirmationListKind, AffirmationRepository>? = null
    @Volatile
    private var affirmationReviewSessionRepositories:
        MutableMap<AffirmationListKind, AffirmationReviewSessionRepository>? = null
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
    private var heartsRepository: HeartsRepository? = null

    @Volatile
    private var tabBackgroundRotation: TabBackgroundRotation? = null

    @Volatile
    private var oneNotePreferences: OneNotePreferences? = null

    @Volatile
    private var oneNoteAuthManager: OneNoteAuthManager? = null

    @Volatile
    private var oneNoteSyncRepository: OneNoteSyncRepository? = null

    @Volatile
    private var moodTrackerRepository: MoodTrackerRepository? = null

    @Volatile
    private var moodTrackerPreferences: MoodTrackerPreferences? = null

    @Volatile
    private var livingTreeRepository: LivingTreeRepository? = null

    @Volatile
    private var homeActivityRepository: HomeActivityRepository? = null

    @Volatile
    private var autoBackupPreferences: AutoBackupPreferences? = null

    @Volatile
    private var autoBackupManager: AutoBackupManager? = null

    @Volatile
    private var autoBackupScheduler: AutoBackupScheduler? = null

    fun autoBackupPreferences(context: Context): AutoBackupPreferences =
        autoBackupPreferences ?: synchronized(this) {
            autoBackupPreferences ?: AutoBackupPreferences(
                context.applicationContext,
            ).also { autoBackupPreferences = it }
        }

    fun autoBackup(context: Context): AutoBackupManager =
        autoBackupManager ?: synchronized(this) {
            autoBackupManager ?: AutoBackupManager(
                context.applicationContext,
                autoBackupPreferences(context),
            ).also { autoBackupManager = it }
        }

    fun autoBackupScheduler(context: Context): AutoBackupScheduler =
        autoBackupScheduler ?: synchronized(this) {
            autoBackupScheduler ?: AutoBackupScheduler(
                context.applicationContext,
            ).also { autoBackupScheduler = it }
        }

    fun homeActivity(context: Context): HomeActivityRepository =
        homeActivityRepository ?: synchronized(this) {
            homeActivityRepository ?: HomeActivityRepository(
                SereneDatabase.getInstance(context.applicationContext),
            ).also { homeActivityRepository = it }
        }

    fun affirmations(
        context: Context,
        listKind: AffirmationListKind = AffirmationListKind.Affirmations,
    ): AffirmationRepository =
        synchronized(this) {
            val map = affirmationRepositories ?: mutableMapOf<AffirmationListKind, AffirmationRepository>()
                .also { affirmationRepositories = it }
            map.getOrPut(listKind) {
                AffirmationRepository(
                    dao = SereneDatabase.getInstance(context.applicationContext).affirmationDao(),
                    listKind = listKind,
                )
            }
        }

    fun affirmationReviewSessions(
        context: Context,
        listKind: AffirmationListKind = AffirmationListKind.Affirmations,
    ): AffirmationReviewSessionRepository =
        synchronized(this) {
            val map = affirmationReviewSessionRepositories
                ?: mutableMapOf<AffirmationListKind, AffirmationReviewSessionRepository>()
                    .also { affirmationReviewSessionRepositories = it }
            map.getOrPut(listKind) {
                AffirmationReviewSessionRepository(
                    dao = SereneDatabase.getInstance(context.applicationContext).affirmationReviewSessionDao(),
                    moodTracker = moodTracker(context),
                    listKind = listKind,
                )
            }
        }

    fun thoughtDumps(context: Context): ThoughtDumpRepository =
        thoughtDumpRepository ?: synchronized(this) {
            thoughtDumpRepository ?: ThoughtDumpRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).thoughtDumpDao(),
                moodTracker = moodTracker(context),
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
                dao = SereneDatabase.getInstance(context.applicationContext).meditationReflectionDao(),
                moodTracker = moodTracker(context),
            ).also { meditationReflectionRepository = it }
        }

    fun futureSelfMessages(context: Context): FutureSelfMessageRepository =
        futureSelfMessageRepository ?: synchronized(this) {
            futureSelfMessageRepository ?: FutureSelfMessageRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).futureSelfMessageDao(),
                moodTracker = moodTracker(context),
            ).also { futureSelfMessageRepository = it }
        }

    fun refactoringEntries(context: Context): RefactoringRepository =
        refactoringRepository ?: synchronized(this) {
            refactoringRepository ?: RefactoringRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).refactoringEntryDao(),
                moodTracker = moodTracker(context),
            ).also { refactoringRepository = it }
        }

    fun centerOfGravityEntries(context: Context): CenterOfGravityRepository =
        centerOfGravityRepository ?: synchronized(this) {
            centerOfGravityRepository ?: CenterOfGravityRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).centerOfGravityEntryDao(),
                moodTracker = moodTracker(context),
            ).also { centerOfGravityRepository = it }
        }

    fun nvcEntries(context: Context): NvcRepository =
        nvcRepository ?: synchronized(this) {
            nvcRepository ?: NvcRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).nvcEntryDao(),
                moodTracker = moodTracker(context),
            ).also { nvcRepository = it }
        }

    fun heartsEntries(context: Context): HeartsRepository =
        heartsRepository ?: synchronized(this) {
            heartsRepository ?: HeartsRepository(
                dao = SereneDatabase.getInstance(context.applicationContext).heartsEntryDao(),
                moodTracker = moodTracker(context),
            ).also { heartsRepository = it }
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

    fun moodTracker(context: Context): MoodTrackerRepository =
        moodTrackerRepository ?: synchronized(this) {
            val database = SereneDatabase.getInstance(context.applicationContext)
            moodTrackerRepository ?: MoodTrackerRepository(
                moodEntryDao = database.moodEntryDao(),
                thoughtDumpDao = database.thoughtDumpDao(),
                meditationReflectionDao = database.meditationReflectionDao(),
                futureSelfMessageDao = database.futureSelfMessageDao(),
                refactoringEntryDao = database.refactoringEntryDao(),
                centerOfGravityEntryDao = database.centerOfGravityEntryDao(),
                nvcEntryDao = database.nvcEntryDao(),
            ).also { moodTrackerRepository = it }
        }

    fun moodTrackerPreferences(context: Context): MoodTrackerPreferences =
        moodTrackerPreferences ?: synchronized(this) {
            moodTrackerPreferences ?: MoodTrackerPreferences(context.applicationContext)
                .also { moodTrackerPreferences = it }
        }

    fun livingTree(context: Context): LivingTreeRepository =
        livingTreeRepository ?: synchronized(this) {
            val database = SereneDatabase.getInstance(context.applicationContext)
            livingTreeRepository ?: LivingTreeRepository(
                tagDao = database.livingTreeTagDao(),
                personDao = database.livingTreePersonDao(),
                personTagDao = database.livingTreePersonTagDao(),
            ).also { livingTreeRepository = it }
        }
}
