package com.example.meditationparticles.data.update

import com.example.meditationparticles.domain.update.ReleaseManifest
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class UpdateManifestClient(
    private val manifestUrl: String,
    private val httpClient: OkHttpClient = defaultClient(),
) {
    fun fetch(): ReleaseManifest {
        val request = Request.Builder()
            .url(cacheBustedUrl(manifestUrl))
            .get()
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
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

        internal fun cacheBustedUrl(baseUrl: String): String {
            val httpUrl = baseUrl.toHttpUrlOrNull() ?: return baseUrl
            return httpUrl.newBuilder()
                .addQueryParameter("_cb", System.currentTimeMillis().toString())
                .build()
                .toString()
        }
    }
}
