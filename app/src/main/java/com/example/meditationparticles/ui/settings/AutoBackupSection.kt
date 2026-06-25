package com.example.meditationparticles.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.data.backup.AutoBackupSnapshot
import com.example.meditationparticles.domain.backup.AutoBackupFrequency
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AutoBackupSection(
    snapshot: AutoBackupSnapshot,
    isRunningBackup: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    onFrequencySelected: (AutoBackupFrequency) -> Unit,
    onChooseFolder: () -> Unit,
    onBackupNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        Text(
            text = "Protect your data",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Uninstalling Sway deletes everything stored inside the app. " +
                "Turn on automatic backups to a folder outside the app (for example Documents/Sway Backups) " +
                "so your journals survive reinstall. With Android backup enabled on your device, " +
                "journal data is also included in Google's encrypted device backup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsToggleRow(
                    label = "Automatic backup",
                    icon = Icons.Default.Backup,
                    checked = snapshot.autoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange,
                )
                Text(
                    text = if (snapshot.isConfigured) {
                        "Folder selected. Sway keeps the latest 14 JSON backups."
                    } else {
                        "Choose a folder before enabling automatic backup."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onChooseFolder,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text("Choose backup folder")
                }
                if (snapshot.autoBackupEnabled) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AutoBackupFrequency.entries.forEach { frequency ->
                            ThemeModeChip(
                                label = frequency.label,
                                selected = snapshot.frequency == frequency,
                                onClick = { onFrequencySelected(frequency) },
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onBackupNow,
                    enabled = snapshot.isConfigured && !isRunningBackup,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (isRunningBackup) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Back up now")
                    }
                }
                snapshot.lastBackupAtMillis?.let { millis ->
                    Text(
                        text = "Last backup: ${formatBackupTimestamp(millis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                snapshot.lastBackupMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatBackupTimestamp(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.getDefault())
    return formatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}
