package com.example.meditationparticles.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataImportTest {
    @Test
    fun validateExportJson_readsVersionFromSampleExport() {
        val json = """
            {
              "exportVersion": 1,
              "exportedAt": "2026-05-25T12:00:00Z",
              "configuration": {},
              "entries": {
                "affirmations": [
                  {
                    "id": 1,
                    "text": "I am calm",
                    "createdAt": 1710000000000,
                    "sortOrder": 0,
                    "isFavorite": false
                  }
                ]
              }
            }
        """.trimIndent()

        assertEquals(1, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun validateExportJson_defaultsMissingVersionToCurrent() {
        val json = """{"configuration":{},"entries":{}}"""

        assertEquals(AppDataExporter.EXPORT_VERSION, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun buildSummary_listsImportedCountsAndSkips() {
        val result = ImportResult(
            counts = ImportCounts(
                experienceSettings = 1,
                affirmations = 2,
                thoughtDumps = 1,
                futureSelfMessages = 1,
            ),
            skips = listOf(
                ImportSkip("future self message", "audio file missing (text imported)"),
                ImportSkip("thought dump", "duplicate", detail = "Repeated entry"),
            ),
            warnings = listOf("Backup version 1 is older; compatible fields were imported."),
        )

        val summary = result.buildSummary()

        assertTrue(summary.contains("Imported:"))
        assertTrue(summary.contains("2 affirmations"))
        assertTrue(summary.contains("Skipped:"))
        assertTrue(summary.contains("audio file missing"))
        assertTrue(summary.contains("Backup version 1 is older"))
    }

    @Test(expected = ImportParseException::class)
    fun parseExportDocument_rejectsMalformedJson() {
        AppDataImporter.parseExportDocument("{ not valid json")
    }

    @Test(expected = ImportParseException::class)
    fun parseExportDocument_rejectsEmptyFile() {
        AppDataImporter.parseExportDocument("   ")
    }
}
