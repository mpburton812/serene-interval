package com.safehaven.affirmations.ui.mood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.domain.mood.MoodGraphPeriod
import com.safehaven.affirmations.domain.mood.MoodMonthGraphMode
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.util.Locale
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodGraphScreen(
    period: MoodGraphPeriod,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as android.app.Application
    val viewModel: MoodGraphViewModel = viewModel(key = "mood_graph_${period.name}") {
        MoodGraphViewModel(app, period)
    }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = period.screenTitle(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
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
                title = state.periodTitle,
                onPrevious = { viewModel.shiftPeriod(forward = false) },
                onNext = { viewModel.shiftPeriod(forward = true) },
                canGoForward = state.canGoForward,
            )

            if (period == MoodGraphPeriod.MONTH) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.monthGraphMode == MoodMonthGraphMode.TOTAL_AVERAGE,
                        onClick = { viewModel.setMonthGraphMode(MoodMonthGraphMode.TOTAL_AVERAGE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text("Month average")
                    }
                    SegmentedButton(
                        selected = state.monthGraphMode == MoodMonthGraphMode.ROLLING_7_DAY,
                        onClick = { viewModel.setMonthGraphMode(MoodMonthGraphMode.ROLLING_7_DAY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("7-day rolling")
                    }
                }
            }

            Text(
                text = state.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                MoodLineGraph(
                    entries = state.entries,
                    period = period,
                    startMillis = state.startMillis,
                    endMillis = state.endMillis,
                    graphEndMillis = state.graphEndMillis,
                    average = state.average,
                    monthGraphMode = state.monthGraphMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }

            if (state.entries.isEmpty()) {
                Text(
                    text = "No mood entries for this period yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

private fun MoodGraphPeriod.screenTitle(): String = when (this) {
    MoodGraphPeriod.DAY -> "Day"
    MoodGraphPeriod.WEEK -> "Week"
    MoodGraphPeriod.MONTH -> "Month"
    MoodGraphPeriod.CALENDAR -> "Calendar"
}

private fun MoodGraphUiState.subtitle(): String {
    val count = entries.size
    if (count == 0) return "No entries"
    val avgText = average?.let { String.format(Locale.getDefault(), "%.1f", round(it * 10) / 10.0) }
        ?: "—"
    val modeLabel = if (monthGraphMode == MoodMonthGraphMode.ROLLING_7_DAY) {
        " · 7-day rolling"
    } else {
        ""
    }
    return "$count entr${if (count == 1) "y" else "ies"} · avg $avgText$modeLabel"
}
