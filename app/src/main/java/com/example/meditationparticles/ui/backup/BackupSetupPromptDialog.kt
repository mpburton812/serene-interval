package com.example.meditationparticles.ui.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign

@Composable
fun BackupSetupPromptDialog(
    onEnableBackup: () -> Unit,
    onDismiss: () -> Unit,
    onDismissPermanently: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Back up your Sway",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = "You have journal entries in Sway. If you uninstall the app, that data is deleted.\n\n" +
                        "Choose a backup folder and Sway can save automatic JSON backups there.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
                TextButton(onClick = onDismissPermanently) {
                    Text("Don't ask again")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onEnableBackup) {
                Text("Set up backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not now")
            }
        },
    )
}
