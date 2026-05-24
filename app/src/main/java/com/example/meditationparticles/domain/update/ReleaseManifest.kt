package com.example.meditationparticles.domain.update

data class ReleaseManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val minVersionCode: Int,
)

enum class UpdateComparison {
    UpToDate,
    OptionalUpdate,
    RequiredUpdate,
}

fun compareVersions(installedVersionCode: Int, manifest: ReleaseManifest): UpdateComparison {
    if (installedVersionCode >= manifest.versionCode) {
        return UpdateComparison.UpToDate
    }
    return if (installedVersionCode < manifest.minVersionCode) {
        UpdateComparison.RequiredUpdate
    } else {
        UpdateComparison.OptionalUpdate
    }
}
