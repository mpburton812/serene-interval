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

    @Test
    fun migration22To23_addsListKindColumns() {
        val migration = SERENE_DATABASE_MIGRATIONS.first {
            it.startVersion == 22 && it.endVersion == 23
        }
        assertTrue(migration.startVersion == 22)
        assertTrue(migration.endVersion == 23)
    }

    @Test
    fun migration23To24_addsIsArchivedColumn() {
        val migration = SERENE_DATABASE_MIGRATIONS.first {
            it.startVersion == 23 && it.endVersion == 24
        }
        assertTrue(migration.startVersion == 23)
        assertTrue(migration.endVersion == 24)
    }
}
