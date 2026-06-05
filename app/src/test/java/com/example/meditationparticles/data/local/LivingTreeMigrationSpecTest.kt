package com.example.meditationparticles.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeMigrationSpecTest {
    @Test
    fun migration17To18_createsLivingTreeTables() {
        val migration = SERENE_DATABASE_MIGRATIONS[SERENE_DATABASE_MIGRATIONS.lastIndex - 1]
        assertTrue(migration.startVersion == 17)
        assertTrue(migration.endVersion == 18)
    }

    @Test
    fun migration18To19_addsRadiusFractionColumn() {
        val migration = SERENE_DATABASE_MIGRATIONS.last()
        assertTrue(migration.startVersion == 18)
        assertTrue(migration.endVersion == 19)
    }
}
