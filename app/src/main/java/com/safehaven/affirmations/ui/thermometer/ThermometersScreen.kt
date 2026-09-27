package com.safehaven.affirmations.ui.thermometer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.data.local.ThermometerEventEntity
import com.safehaven.affirmations.data.local.ThermometerEventType
import com.safehaven.affirmations.domain.thermometer.ThermometerRules
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.PersistentEntryDialogProperties
import com.safehaven.affirmations.ui.components.ReportEntryPopup
import com.safehaven.affirmations.ui.components.SereneTabBackground
import com.safehaven.affirmations.ui.components.SereneTabHeader
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.text.DateFormat
import java.util.Date

@Composable
fun ThermometersScreen(
    modifier: Modifier = Modifier,
    viewModel: ThermometersViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    ReportEntryPopup(id = "thermometers-editor", active = draft.open)

    SereneTabBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            SereneTabHeader(title = "Thermometers")
            Text(
                text = "Track up to ${ThermometerRules.MAX_ACTIVE} stress levels, from calm blue to hot red.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.active.isEmpty()) {
                Text(
                    text = "No thermometers yet.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            state.active.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
                ) {
                    rowItems.forEach { thermometer ->
                        ThermometerCard(
                            name = thermometer.name,
                            score = state.latestScore(thermometer.id),
                            onClick = { viewModel.open(thermometer.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            TextButton(
                onClick = viewModel::startCreate,
                enabled = state.canCreate,
            ) {
                Text(if (state.canCreate) "Add thermometer" else "Five thermometers already active")
            }
            if (state.archived.isNotEmpty()) {
                Text("Archived", style = MaterialTheme.typography.titleMedium)
                state.archived.forEach { thermometer ->
                    Text(
                        text = thermometer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (draft.open) {
        ThermometerEditorDialog(
            state = state,
            draft = draft,
            onNameChange = viewModel::updateName,
            onScoreChange = viewModel::updateScore,
            onNoteChange = viewModel::updateNote,
            onSaveName = viewModel::saveName,
            onRecord = viewModel::recordTemperature,
            onArchive = viewModel::archive,
            onDelete = viewModel::delete,
            onClose = viewModel::closeEditor,
        )
    }
}

@Composable
private fun ThermometerCard(
    name: String,
    score: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 20.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThermometerGraphic(score = score)
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = score?.toString() ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = score?.let(::thermometerColor) ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThermometerEditorDialog(
    state: ThermometersUiState,
    draft: EditorDraft,
    onNameChange: (String) -> Unit,
    onScoreChange: (Float) -> Unit,
    onNoteChange: (String) -> Unit,
    onSaveName: () -> Unit,
    onRecord: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val events = draft.editingId?.let(state::eventsFor).orEmpty()
    val readings = events.filter { it.type == ThermometerEventType.READING }
    AlertDialog(
        onDismissRequest = {},
        properties = PersistentEntryDialogProperties,
        title = { Text(if (draft.creating) "New thermometer" else "Thermometer") },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.draftName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                )
                TextButton(onClick = onSaveName, enabled = draft.draftName.isNotBlank()) {
                    Text(if (draft.creating) "Create" else "Save name")
                }
                if (draft.editingId != null) {
                    Text("Stress level", style = MaterialTheme.typography.titleSmall)
                    StressGraph(readings = readings)
                    Text("Score by reading: ${draft.draftScore.toInt()}")
                    Slider(
                        value = draft.draftScore,
                        onValueChange = onScoreChange,
                        valueRange = ThermometerRules.MIN_SCORE.toFloat()..ThermometerRules.MAX_SCORE.toFloat(),
                    )
                    OutlinedTextField(
                        value = draft.draftNote,
                        onValueChange = onNoteChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Why did it change? (optional)") },
                        minLines = 2,
                    )
                    TextButton(onClick = onRecord) {
                        Text("Record temperature")
                    }
                    Text("History", style = MaterialTheme.typography.titleSmall)
                    if (events.isEmpty()) {
                        Text("No temperatures yet.", style = MaterialTheme.typography.bodySmall)
                    }
                    events.forEach { event ->
                        Text(
                            text = historyLine(event),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onArchive) { Text("Archive") }
                        TextButton(onClick = onDelete) { Text("Delete") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

@Composable
private fun StressGraph(readings: List<ThermometerEventEntity>) {
    val points = remember(readings) {
        readings.sortedBy { it.recordedAt }.mapNotNull { event ->
            event.score?.toFloat()
        }
    }
    if (points.isEmpty()) {
        Text("No graph yet.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val line = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val path = Path()
        points.forEachIndexed { index, score ->
            val x = if (points.size == 1) size.width / 2f else size.width * index / (points.size - 1)
            val y = size.height * (1f - score / ThermometerRules.MAX_SCORE)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round))
        points.forEachIndexed { index, score ->
            val x = if (points.size == 1) size.width / 2f else size.width * index / (points.size - 1)
            val y = size.height * (1f - score / ThermometerRules.MAX_SCORE)
            drawCircle(thermometerColor(score.toInt()), radius = 6f, center = Offset(x, y))
        }
    }
}

private fun historyLine(event: ThermometerEventEntity): String {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(event.recordedAt))
    return when (event.type) {
        ThermometerEventType.RENAME -> {
            val from = event.previousName?.ifBlank { "untitled" } ?: "untitled"
            "$whenText · Renamed from $from to ${event.newName.orEmpty()}"
        }
        else -> {
            val why = event.note?.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
            "$whenText · ${event.score ?: "—"}°$why"
        }
    }
}
