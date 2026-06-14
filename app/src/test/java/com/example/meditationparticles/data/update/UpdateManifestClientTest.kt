package com.example.meditationparticles.data.update

import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManifestClientTest {
    @Test
    fun cacheBustedUrl_appendsQueryParameter() {
        val url = UpdateManifestClient.cacheBustedUrl(
            "https://raw.githubusercontent.com/org/repo/main/release/version.json",
        )

        assertTrue(url.contains("_cb="))
        assertTrue(url.startsWith("https://raw.githubusercontent.com/org/repo/main/release/version.json?"))
    }
}
