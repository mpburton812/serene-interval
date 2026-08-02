package com.safehaven.affirmations.data.backup

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AutoBackupManagerTest {
    @Test
    fun buildBackupFileName_usesTimestampPattern() {
        val name = AutoBackupManager.buildBackupFileName(Instant.parse("2026-06-25T18:30:00Z"))

        assertTrue(name.startsWith("sway-backup-"))
        assertTrue(name.endsWith(".json"))
        assertTrue(name.matches(Regex("sway-backup-\\d{4}-\\d{2}-\\d{2}-\\d{4}\\.json")))
    }
}
