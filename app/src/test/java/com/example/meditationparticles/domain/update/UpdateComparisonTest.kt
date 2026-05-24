package com.example.meditationparticles.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateComparisonTest {
    @Test
    fun upToDateWhenInstalledMatchesOrExceedsRemote() {
        val manifest = ReleaseManifest(
            versionCode = 5,
            versionName = "1.5",
            apkUrl = "https://example.com/app.apk",
            releaseNotes = "",
            minVersionCode = 3,
        )
        assertEquals(UpdateComparison.UpToDate, compareVersions(5, manifest))
        assertEquals(UpdateComparison.UpToDate, compareVersions(6, manifest))
    }

    @Test
    fun optionalUpdateWhenBehindButAboveMinimum() {
        val manifest = ReleaseManifest(
            versionCode = 5,
            versionName = "1.5",
            apkUrl = "https://example.com/app.apk",
            releaseNotes = "",
            minVersionCode = 3,
        )
        assertEquals(UpdateComparison.OptionalUpdate, compareVersions(4, manifest))
    }

    @Test
    fun requiredUpdateWhenBelowMinimum() {
        val manifest = ReleaseManifest(
            versionCode = 10,
            versionName = "2.0",
            apkUrl = "https://example.com/app.apk",
            releaseNotes = "",
            minVersionCode = 8,
        )
        assertEquals(UpdateComparison.RequiredUpdate, compareVersions(7, manifest))
    }
}
