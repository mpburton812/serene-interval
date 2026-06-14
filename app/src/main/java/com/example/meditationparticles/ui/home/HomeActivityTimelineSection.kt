package com.example.meditationparticles.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.home.HomeActivityItem
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun HomeActivityTimelineSection(
    activities: List<HomeActivityItem>,
    onOpenTextEntry: (HomeActivityItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (activities.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "Recent activity",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        activities.forEach { activity ->
            HomeActivityRow(
                activity = activity,
                onOpenTextEntry = onOpenTextEntry,
            )
        }
    }
}

@Composable
private fun HomeActivityRow(
    activity: HomeActivityItem,
    onOpenTextEntry: (HomeActivityItem) -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (activity.textEntry != null) {
                    Modifier.clickable { onOpenTextEntry(activity) }
                } else {
                    Modifier
                },
            ),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            activity.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = formatActivityTime(activity.completedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            activity.textEntry?.let { preview ->
                Text(
                    text = "View entry: ${preview.lineSequence().first()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
fun HomeActivityTextDialog(
    activity: HomeActivityItem,
    onDismiss: () -> Unit,
) {
    val text = activity.textEntry ?: return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(activity.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activity.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun formatActivityTime(completedAt: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        completedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}
