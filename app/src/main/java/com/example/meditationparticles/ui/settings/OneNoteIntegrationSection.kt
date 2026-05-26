package com.example.meditationparticles.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.BuildConfig
import com.example.meditationparticles.data.onenote.OneNotePrefsSnapshot
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OneNoteIntegrationSection(
    prefs: OneNotePrefsSnapshot,
    uiState: OneNoteSettingsUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncEnabledChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "OneNote",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!BuildConfig.ONENOTE_SYNC_AVAILABLE) {
            Text(
                text = "OneNote sync is not configured for this build. Add onenote.clientId to " +
                    "local.properties and register the app in Azure — see release/ONENOTE.md.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Text(
            text = "Optionally mirror saved toolkit journal entries to a OneNote section. " +
                "Audio recordings stay in the app only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val connected = !prefs.accountEmail.isNullOrBlank()
        Text(
            text = if (connected) {
                "Connected as ${prefs.accountEmail}"
            } else {
                "Not connected"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (connected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        prefs.lastSyncAtMillis?.let { millis ->
            Text(
                text = "Last sync: ${formatSyncTime(millis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        (uiState.errorMessage ?: prefs.lastError)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (connected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Auto-sync on save",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = prefs.syncEnabled,
                    onCheckedChange = onSyncEnabledChange,
                    enabled = !uiState.isBusy,
                )
            }
        }

        OutlinedButton(
            onClick = if (connected) onDisconnect else onConnect,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (uiState.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 8.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (connected) "Disconnect" else "Connect Microsoft account",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        if (connected) {
            OutlinedButton(
                onClick = onSyncNow,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Sync now",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

private fun formatSyncTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

data class OneNoteSettingsUiState(
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)
