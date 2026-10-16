package com.safehaven.affirmations.data

import android.content.Context
import com.safehaven.affirmations.data.backup.AutoBackupPreferences
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind
import com.safehaven.affirmations.widget.MoodWidgetPreferences

/**
 * Single entry point for the app's SharedPreferences stores.
 * Each store keeps its own file; this only stops call sites from constructing them ad hoc.
 */
class UserPreferences(
    context: Context,
) {
    private val appContext = context.applicationContext

    val settings: SettingsPreferences get() = AppGraph.settings(appContext)
    val toolkit: ToolkitPreferences get() = AppGraph.toolkit(appContext)
    val quickStart: QuickStartPreferences get() = AppGraph.quickStart(appContext)
    val moodTracker: MoodTrackerPreferences get() = AppGraph.moodTrackerPreferences(appContext)
    val autoBackup: AutoBackupPreferences get() = AppGraph.autoBackupPreferences(appContext)
    val tabBackground: TabBackgroundRotation get() = AppGraph.tabBackgroundRotation(appContext)

    val timer: TimerPreferences by lazy { TimerPreferences(appContext) }
    val breathingDisplay: BreathingDisplayPreferences by lazy { BreathingDisplayPreferences(appContext) }
    val moodWidget: MoodWidgetPreferences by lazy { MoodWidgetPreferences(appContext) }

    fun affirmations(
        listKind: AffirmationListKind = AffirmationListKind.Affirmations,
    ): AffirmationPreferences = AffirmationPreferences(appContext, listKind)
}
