package com.example.meditationparticles.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseManifestParserTest {
    @Test
    fun parsesManifestFromJson() {
        val json = """
            {
              "versionCode": 12,
              "versionName": "1.2.0",
              "apkUrl": "https://example.com/app.apk",
              "releaseNotes": "Bug fixes",
              "minVersionCode": 10
            }
        """.trimIndent()

        val manifest = ReleaseManifestParser.parse(json)

        assertEquals(12, manifest.versionCode)
        assertEquals("1.2.0", manifest.versionName)
        assertEquals("https://example.com/app.apk", manifest.apkUrl)
        assertEquals("Bug fixes", manifest.releaseNotes)
        assertEquals(10, manifest.minVersionCode)
    }

    @Test
    fun defaultsMinVersionCodeToLatestVersionCode() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.0.2",
              "apkUrl": "https://example.com/app.apk"
            }
        """.trimIndent()

        val manifest = ReleaseManifestParser.parse(json)

        assertEquals(3, manifest.minVersionCode)
    }
}
