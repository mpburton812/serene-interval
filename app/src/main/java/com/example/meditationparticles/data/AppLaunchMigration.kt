package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.BuildConfig

data class AppLaunchMigrationResult(
    val resetNavigationToHome: Boolean,
)

/**
 * Runs once per process start. Detects APK upgrades and repairs persisted state that can
 * crash startup when older releases are updated in place (e.g. v1.0.23 → v1.0.30).
 */
object AppLaunchMigration {
    private const val PREFS_NAME = "app_launch_migration"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"

    fun run(context: Context): AppLaunchMigrationResult {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previousVersionCode = prefs.getInt(KEY_LAST_VERSION_CODE, 0)
        val currentVersionCode = BuildConfig.VERSION_CODE

        val upgraded = previousVersionCode in 1 until currentVersionCode
        if (upgraded) {
            migrateTabBackgroundRotation(appContext)
        }

        if (previousVersionCode != currentVersionCode) {
            prefs.edit()
                .putInt(KEY_LAST_VERSION_CODE, currentVersionCode)
                .apply()
        }

        // v1.0.27+ rendered Visualizations in both NavHost and the tab pager when the route
        // was restored after upgrade. Reset to Home on any upgrade until state stabilizes.
        return AppLaunchMigrationResult(resetNavigationToHome = upgraded)
    }

    internal fun legacyTabBackgroundIndexMigrations(
        dayIndex: Int?,
        nightIndex: Int?,
    ): Map<String, Int> = buildMap {
        dayIndex?.takeIf { it >= 0 }?.let { put("index_Classic_Daylight", it) }
        nightIndex?.takeIf { it >= 0 }?.let { put("index_Classic_Nighttime", it) }
    }

    private fun migrateTabBackgroundRotation(context: Context) {
        val rotationPrefs = context.getSharedPreferences("tab_background_rotation", Context.MODE_PRIVATE)
        if (!rotationPrefs.contains("day_index") && !rotationPrefs.contains("night_index")) return

        val editor = rotationPrefs.edit()
        legacyTabBackgroundIndexMigrations(
            dayIndex = rotationPrefs.getInt("day_index", -1),
            nightIndex = rotationPrefs.getInt("night_index", -1),
        ).forEach { (key, value) -> editor.putInt(key, value) }
        editor.apply()
    }
}
