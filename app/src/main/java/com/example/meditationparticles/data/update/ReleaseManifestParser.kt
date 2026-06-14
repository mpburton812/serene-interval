package com.example.meditationparticles.data.update

import com.example.meditationparticles.domain.update.ReleaseManifest
import org.json.JSONObject

object ReleaseManifestParser {
    fun parse(json: String): ReleaseManifest {
        val root = JSONObject(stripUtf8Bom(json))
        return ReleaseManifest(
            versionCode = root.getInt("versionCode"),
            versionName = root.getString("versionName"),
            apkUrl = root.getString("apkUrl"),
            releaseNotes = root.optString("releaseNotes", ""),
            minVersionCode = root.optInt("minVersionCode", root.getInt("versionCode")),
            expectedSha256 = root.optString("expectedSha256").takeIf { it.isNotBlank() },
        )
    }

    private fun stripUtf8Bom(json: String): String =
        if (json.isNotEmpty() && json.first() == '\uFEFF') json.drop(1) else json
}
