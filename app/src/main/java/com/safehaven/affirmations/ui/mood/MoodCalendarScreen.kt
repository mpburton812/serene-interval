package com.safehaven.affirmations.ui.mood

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.domain.mood.MoodCalendarEvent
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.MoodEntryIcon
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodCalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: MoodCalendarViewModel = viewModel(key = "mood_calendar") {
        MoodCalendarViewModel(app)
    }
    val state by viewModel.uiState.collectAsState()
    val daySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            MoodPeriodNavigator(
                title = state.monthTitle,
                onPrevious = { viewModel.shiftMonth(forward = false) },
                onNext = { viewModel.shiftMonth(forward = true) },
                canGoForward = state.canGoForward,
            )

            Text(
                text = "Tap a day to see its events. Each day is colored by your average mood.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                MoodCalendarGrid(
                    days = state.days,
                    weekdayHeaders = state.weekdayHeaders,
                    onDayClick = { day ->
                        if (day.inMonth) {
                            viewModel.selectDay(day.date)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            MoodCalendarLegend()

            if (state.days.none { it.inMonth && it.average != null }) {
                Text(
                    text = "No mood entries for this month yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val selectedDay = state.selectedDay
    if (selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissDay() },
            sheetState = daySheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SereneSpacing.containerMargin)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = DateTimeFormatter.ofPattern("EEEE, MMMM d", locale).format(selectedDay),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.selectedDayEvents.isEmpty()) {
                    Text(
                        text = "No events recorded for this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.selectedDayEvents.forEach { event ->
                        MoodCalendarEventRow(
                            event = event,
                            onClick = { viewModel.openEvent(event) },
                        )
                    }
                }
            }
        }
    }

    state.selectedEvent?.let { event ->
        MoodCalendarEventDialog(
            event = event,
            onDismiss = { viewModel.dismissEvent() },
        )
    }
}

@Composable
private fun MoodCalendarEventRow(
    event: MoodCalendarEvent,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoodEntryIcon(moodLevel = event.moodLevel)
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            event.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = formatEventTime(event.completedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (event.detail != null) {
                Text(
                    text = "Tap for details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MoodCalendarEventDialog(
    event: MoodCalendarEvent,
    onDismiss: () -> Unit,
) {
    val detailText = event.detail ?: buildMoodDetail(event)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                event.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = formatEventTime(event.completedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 20,
                    overflow = TextOverflow.Ellipsis,
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

private fun buildMoodDetail(event: MoodCalendarEvent): String {
    val moodLabel = event.moodLevel?.let(MoodScale::label) ?: "Unknown"
    return "Mood: $moodLabel"
}

private fun formatEventTime(completedAt: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        completedAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
}
