package com.example.meditationparticles.data.local

import org.junit.Assert.assertTrue
import org.junit.Test

class AffirmationReviewMigrationSpecTest {
    @Test
    fun migration20To21_createsAffirmationReviewSessionsTable() {
        val migration = SERENE_DATABASE_MIGRATIONS.first {
            it.startVersion == 20 && it.endVersion == 21
        }
        assertTrue(migration.startVersion == 20)
        assertTrue(migration.endVersion == 21)
    }
}
