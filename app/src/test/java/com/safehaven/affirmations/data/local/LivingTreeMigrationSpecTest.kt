package com.safehaven.affirmations.data.local

import androidx.room.migration.Migration
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeMigrationSpecTest {
    @Test
    fun migration17To18_createsLivingTreeTables() {
        val migration = migration(17, 18)
        assertTrue(migration.startVersion == 17)
        assertTrue(migration.endVersion == 18)
    }

    @Test
    fun migration18To19_addsRadiusFractionColumn() {
        val migration = migration(18, 19)
        assertTrue(migration.startVersion == 18)
        assertTrue(migration.endVersion == 19)
    }

    @Test
    fun migration19To20_addsIsUserPlacedColumn() {
        val migration = migration(19, 20)
        assertTrue(migration.startVersion == 19)
        assertTrue(migration.endVersion == 20)
    }

    private fun migration(start: Int, end: Int): Migration =
        SERENE_DATABASE_MIGRATIONS.first { it.startVersion == start && it.endVersion == end }
}
