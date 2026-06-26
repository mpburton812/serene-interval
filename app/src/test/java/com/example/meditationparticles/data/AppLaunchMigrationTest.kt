package com.example.meditationparticles.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLaunchMigrationTest {
    @Test
    fun legacyTabBackgroundIndexMigrations_mapsDayAndNight() {
        val migrated = AppLaunchMigration.legacyTabBackgroundIndexMigrations(
            dayIndex = 3,
            nightIndex = 2,
        )

        assertEquals(3, migrated["index_Classic_Daylight"])
        assertEquals(2, migrated["index_Classic_Nighttime"])
    }

    @Test
    fun legacyTabBackgroundIndexMigrations_ignoresNegativeIndices() {
        val migrated = AppLaunchMigration.legacyTabBackgroundIndexMigrations(
            dayIndex = -1,
            nightIndex = null,
        )

        assertTrue(migrated.isEmpty())
    }
}
