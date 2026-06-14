package com.example.meditationparticles.data.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.example.meditationparticles.domain.update.ReleaseManifest
import java.io.File

object ApkReleaseMetadataVerifier {
    fun verify(context: Context, apkFile: File, manifest: ReleaseManifest) {
        val packageInfo = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_META_DATA,
        ) ?: error("Downloaded APK could not be read.")

        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        val versionName = packageInfo.versionName ?: "unknown"

        if (versionCode < manifest.versionCode) {
            error(
                "Downloaded APK is $versionName (versionCode $versionCode) but the update " +
                    "requires ${manifest.versionName} (versionCode ${manifest.versionCode}). " +
                    "The GitHub release asset is stale; try again later or install from GitHub Releases.",
            )
        }
        if (versionName != manifest.versionName) {
            error(
                "Downloaded APK versionName is $versionName but the update manifest expects " +
                    "${manifest.versionName}. The GitHub release asset is stale; try again later.",
            )
        }
    }
}
