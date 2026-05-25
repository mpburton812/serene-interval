package com.example.meditationparticles.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.BuildConfig
import com.example.meditationparticles.data.update.ApkDownloader
import com.example.meditationparticles.data.update.ApkInstallIntent
import com.example.meditationparticles.data.update.UpdateManifestClient
import com.example.meditationparticles.data.update.readInstalledAppVersion
import com.example.meditationparticles.domain.update.ReleaseManifest
import com.example.meditationparticles.domain.update.UpdateComparison
import com.example.meditationparticles.domain.update.compareVersions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class UpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float? = null,
    val installedVersionName: String = "",
    val manifest: ReleaseManifest? = null,
    val comparison: UpdateComparison? = null,
    val showUpdateDialog: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

class UpdateViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val manifestClient = UpdateManifestClient(BuildConfig.UPDATE_MANIFEST_URL)
    private val apkDownloader = ApkDownloader()

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        val installed = readInstalledAppVersion(application)
        _uiState.update { it.copy(installedVersionName = installed.versionName) }
    }

    fun checkForUpdate(userInitiated: Boolean) {
        if (!BuildConfig.UPDATE_CHECK_ENABLED || _uiState.value.isChecking || _uiState.value.isDownloading) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isChecking = true,
                    statusMessage = null,
                    errorMessage = null,
                )
            }

            runCatching {
                withContext(Dispatchers.IO) {
                    val installed = readInstalledAppVersion(getApplication())
                    val manifest = manifestClient.fetch()
                    val comparison = compareVersions(installed.versionCode, manifest)
                    Triple(installed.versionName, manifest, comparison)
                }
            }.onSuccess { (versionName, manifest, comparison) ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        installedVersionName = versionName,
                        manifest = manifest,
                        comparison = comparison,
                        showUpdateDialog = comparison != UpdateComparison.UpToDate,
                        statusMessage = if (userInitiated && comparison == UpdateComparison.UpToDate) {
                            "You're on the latest version ($versionName)."
                        } else {
                            null
                        },
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        errorMessage = error.message ?: "Could not check for updates.",
                        statusMessage = if (userInitiated) {
                            error.message ?: "Could not check for updates."
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(showUpdateDialog = false) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }

    fun downloadAndInstall() {
        val manifest = _uiState.value.manifest ?: return
        if (_uiState.value.isDownloading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadProgress = 0f,
                    errorMessage = null,
                    showUpdateDialog = false,
                )
            }

            val context = getApplication<Application>()
            val apkFile = File(context.cacheDir, "updates/serene-update.apk")

            runCatching {
                withContext(Dispatchers.IO) {
                    apkDownloader.download(manifest.apkUrl, apkFile) { progress ->
                        _uiState.update { state ->
                            state.copy(downloadProgress = progress)
                        }
                    }
                    apkFile
                }
            }.onSuccess { downloadedApk ->
                _uiState.update {
                    it.copy(isDownloading = false, downloadProgress = null)
                }
                if (!ApkInstallIntent.canRequestPackageInstalls(context)) {
                    ApkInstallIntent.openUnknownSourcesSettings(context)
                    _uiState.update {
                        it.copy(
                            errorMessage = "Allow installs from this app, then try again.",
                        )
                    }
                    return@launch
                }
                context.startActivity(ApkInstallIntent.buildInstallIntent(context, downloadedApk))
                _uiState.update {
                    it.copy(
                        statusMessage = "Follow the system install prompt. If you see \"App not installed\", " +
                            "uninstall Serene Interval first (signature mismatch from an older install), " +
                            "then install again from GitHub Releases or Settings → Check for updates.",
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        downloadProgress = null,
                        errorMessage = error.message ?: "Download failed.",
                    )
                }
            }
        }
    }
}
