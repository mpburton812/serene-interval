package com.example.meditationparticles.data.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat

data class InstalledAppVersion(
    val versionCode: Int,
    val versionName: String,
)

fun readInstalledAppVersion(context: Context): InstalledAppVersion {
    val packageManager = context.packageManager
    val packageInfo = packageManager.getPackageInfo(
        context.packageName,
        PackageManager.GET_META_DATA,
    )
    return InstalledAppVersion(
        versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toInt(),
        versionName = packageInfo.versionName ?: "unknown",
    )
}
