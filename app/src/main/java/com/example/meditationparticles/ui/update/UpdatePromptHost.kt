package com.example.meditationparticles.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.meditationparticles.domain.update.UpdateComparison

@Composable
fun UpdatePromptHost(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val manifest = uiState.manifest
    val comparison = uiState.comparison

    if (uiState.showUpdateDialog && manifest != null && comparison != null &&
        comparison != UpdateComparison.UpToDate
    ) {
        val required = comparison == UpdateComparison.RequiredUpdate
        AlertDialog(
            onDismissRequest = {
                if (!required) {
                    viewModel.dismissUpdateDialog()
                }
            },
            title = { Text("Update available") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Version ${manifest.versionName} is available on main. " +
                            "You have ${uiState.installedVersionName}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (manifest.releaseNotes.isNotBlank()) {
                        Text(
                            text = manifest.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::downloadAndInstall) {
                    Text("Download & install")
                }
            },
            dismissButton = {
                if (!required) {
                    TextButton(onClick = viewModel::dismissUpdateDialog) {
                        Text("Later")
                    }
                }
            },
        )
    }

    if (uiState.isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading update") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Please keep the app open until the download finishes.")
                    val progress = uiState.downloadProgress
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
        )
    }
}
