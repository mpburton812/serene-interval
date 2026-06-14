package com.example.meditationparticles.data.export

import com.example.meditationparticles.domain.mood.MoodScale
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

    @Test
    fun validateExportJson_readsVersionTwo() {
        val json = """{"exportVersion": 2, "configuration": {}, "entries": {}}"""

        assertEquals(2, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun validateExportJson_readsVersionThree() {
        val json = """{"exportVersion": 3, "configuration": {}, "entries": {}}"""

        assertEquals(3, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun validateExportJson_readsVersionFive() {
        val json = """{"exportVersion": 5, "configuration": {}, "entries": {}, "livingTree": {}}"""

        assertEquals(5, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun validateExportJson_readsVersionFour() {
        val json = """{"exportVersion": 4, "configuration": {}, "entries": {}, "livingTree": {}}"""

        assertEquals(4, AppDataImporter.validateExportJson(json))
    }

    @Test
    fun livingTreeExportSection_parsesTagsPeopleAndLinks() {
        val json = """
            {
              "exportVersion": 4,
              "configuration": {},
              "entries": {},
              "livingTree": {
                "tags": [
                  {"id": 1, "name": "Family", "colorArgb": 123, "sortOrder": 0, "createdAtMillis": 1}
                ],
                "people": [
                  {
                    "id": 10,
                    "name": "Alex",
                    "notes": "Close friend",
                    "sortOrder": 0,
                    "createdAtMillis": 2,
                    "updatedAtMillis": 2
                  }
                ],
                "personTags": [
                  {"personId": 10, "tagId": 1}
                ]
              }
            }
        """.trimIndent()

        val root = AppDataImporter.parseExportDocument(json)
        val livingTree = root.getJSONObject("livingTree")

        assertEquals(1, livingTree.getJSONArray("tags").length())
        assertEquals(1, livingTree.getJSONArray("people").length())
        assertEquals(1, livingTree.getJSONArray("personTags").length())
        assertEquals("Alex", livingTree.getJSONArray("people").getJSONObject(0).getString("name"))
    }

    @Test
    fun legacyMoodLevelFiveNormalizesToFour() {
        assertEquals(4, MoodScale.normalize(5))
    }
}

