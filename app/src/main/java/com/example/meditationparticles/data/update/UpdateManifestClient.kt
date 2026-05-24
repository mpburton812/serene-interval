package com.example.meditationparticles.data.update

import com.example.meditationparticles.domain.update.ReleaseManifest
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateManifestClient(
    private val manifestUrl: String,
    private val httpClient: OkHttpClient = defaultClient(),
) {
    fun fetch(): ReleaseManifest {
        val request = Request.Builder()
            .url(manifestUrl)
            .get()
            .header("Cache-Control", "no-cache")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Update check failed (${response.code})")
            }
            val body = response.body?.string() ?: error("Update manifest was empty")
            return ReleaseManifestParser.parse(body)
        }
    }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
