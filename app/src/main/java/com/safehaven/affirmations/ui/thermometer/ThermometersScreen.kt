package com.safehaven.affirmations.ui.thermometer

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.data.local.ThermometerEventEntity
import com.safehaven.affirmations.data.local.ThermometerEventType
import com.safehaven.affirmations.domain.thermometer.ThermometerChangeText
import com.safehaven.affirmations.domain.thermometer.ThermometerGraphStamps
import com.safehaven.affirmations.domain.thermometer.ThermometerLiquid
import com.safehaven.affirmations.domain.thermometer.ThermometerReadingRef
import com.safehaven.affirmations.domain.thermometer.ThermometerRules
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.PersistentEntryDialogProperties
import com.safehaven.affirmations.ui.components.ReportEntryPopup
import com.safehaven.affirmations.ui.components.SereneTabBackground
import com.safehaven.affirmations.ui.components.SereneTabHeader
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun ThermometersScreen(
    modifier: Modifier = Modifier,
    viewModel: ThermometersViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val draft by viewModel.draft.collectAsState()
    val slide by viewModel.slide.collectAsState()
    ReportEntryPopup(id = "thermometers-editor", active = draft.open)
    ReportEntryPopup(id = "thermometers-slide", active = slide != null)

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
                text = "Track up to ${ThermometerRules.MAX_ACTIVE} stress levels. Choose a red or blue liquid for each one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Press and hold a thermometer, then slide up or down to change its level.",
                style = MaterialTheme.typography.bodySmall,
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
                            liquid = ThermometerLiquid.fromStored(thermometer.liquidColor),
                            onClick = { viewModel.open(thermometer.id) },
                            onSlideCommit = { score -> viewModel.commitSlide(thermometer.id, score) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
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
            onReliefChange = viewModel::updateRelief,
            onLiquidChange = viewModel::updateLiquid,
            onSaveName = viewModel::saveName,
            onArchive = viewModel::archive,
            onDelete = viewModel::delete,
            onClose = viewModel::closeEditor,
        )
    }

    slide?.let { capture ->
        SlideNoteDialog(
            capture = capture,
            onNoteChange = viewModel::updateSlideNote,
            onReliefChange = viewModel::updateSlideRelief,
            onClose = viewModel::closeSlide,
        )
    }
}

@Composable
private fun ThermometerCard(
    name: String,
    score: Int?,
    liquid: ThermometerLiquid,
    onClick: () -> Unit,
    onSlideCommit: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragScore by remember(score) { mutableStateOf<Int?>(null) }
    val shown = dragScore ?: score
    GlassCard(
        modifier = modifier
            .clickable(onClick = onClick)
            .pointerInput(score) {
                var dragging = (score ?: 0).toFloat()
                var moved = false
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        dragging = (score ?: 0).toFloat()
                        moved = false
                        dragScore = dragging.roundToInt()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        moved = true
                        val span = size.height.toFloat().coerceAtLeast(1f)
                        dragging = (dragging - dragAmount.y / span * ThermometerRules.MAX_SCORE)
                            .coerceIn(
                                ThermometerRules.MIN_SCORE.toFloat(),
                                ThermometerRules.MAX_SCORE.toFloat(),
                            )
                        dragScore = dragging.roundToInt()
                    },
                    onDragEnd = {
                        val finalScore = dragging.roundToInt()
                        dragScore = null
                        if (moved && finalScore != (score ?: 0)) onSlideCommit(finalScore)
                    },
                    onDragCancel = { dragScore = null },
                )
            },
        cornerRadius = 20.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThermometerGraphic(score = shown, liquid = liquid)
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = shown?.toString() ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = if (shown == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    thermometerMercuryColor(liquid)
                },
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
    onReliefChange: (String) -> Unit,
    onLiquidChange: (ThermometerLiquid) -> Unit,
    onSaveName: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var confirmDelete by remember(draft.editingId, draft.creating) { mutableStateOf(false) }
    val events = draft.editingId?.let(state::eventsFor).orEmpty()
    val readings = events.filter { it.type == ThermometerEventType.READING }
    AlertDialog(
        onDismissRequest = {},
        properties = PersistentEntryDialogProperties,
        title = { Text(if (draft.creating) "New thermometer" else "Thermometer") },
        text = {
            Column(
                modifier = Modifier
                    .height(480.dp)
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
                Text("Liquid", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidChoice(
                        label = "Red",
                        selected = draft.liquid == ThermometerLiquid.RED,
                        onClick = { onLiquidChange(ThermometerLiquid.RED) },
                    )
                    LiquidChoice(
                        label = "Blue",
                        selected = draft.liquid == ThermometerLiquid.BLUE,
                        onClick = { onLiquidChange(ThermometerLiquid.BLUE) },
                    )
                }
                ThermometerGraphic(
                    score = draft.draftScore.roundToInt(),
                    liquid = draft.liquid,
                )
                if (draft.editingId != null) {
                    Text("Stress level", style = MaterialTheme.typography.titleSmall)
                    StressGraph(readings = readings, liquid = draft.liquid)
                    Text("Score by reading: ${draft.draftScore.toInt()}")
                    Slider(
                        value = draft.draftScore,
                        onValueChange = onScoreChange,
                        valueRange = ThermometerRules.MIN_SCORE.toFloat()..ThermometerRules.MAX_SCORE.toFloat(),
                    )
                    ChangeNoteFields(
                        note = draft.draftNote,
                        relief = draft.draftRelief,
                        onNoteChange = onNoteChange,
                        onReliefChange = onReliefChange,
                    )
                    Text("History", style = MaterialTheme.typography.titleSmall)
                    if (events.isEmpty()) {
                        Text("No temperatures yet.", style = MaterialTheme.typography.bodySmall)
                    }
                    val previous = remember(readings) { previousScores(readings) }
                    events.forEach { event ->
                        Text(
                            text = historyLine(event, previous[event.id]),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (draft.editingId != null) {
                    TextButton(onClick = onArchive) { Text("Archive") }
                    TextButton(onClick = { confirmDelete = true }) { Text("Delete") }
                }
                TextButton(onClick = onClose) { Text("Close") }
            }
        },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete thermometer?") },
            text = { Text("This removes the thermometer and its history.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SlideNoteDialog(
    capture: SlideCapture,
    onNoteChange: (String) -> Unit,
    onReliefChange: (String) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = PersistentEntryDialogProperties,
        title = { Text("Thermometer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Stress level ${capture.score}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                ChangeNoteFields(
                    note = capture.note,
                    relief = capture.relief,
                    onNoteChange = onNoteChange,
                    onReliefChange = onReliefChange,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) { Text("Close") }
        },
    )
}

@Composable
private fun ChangeNoteFields(
    note: String,
    relief: String,
    onNoteChange: (String) -> Unit,
    onReliefChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Why did it change? (optional)") },
        minLines = 2,
    )
    OutlinedTextField(
        value = relief,
        onValueChange = onReliefChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("How can you lower this stress?") },
        minLines = 2,
    )
}

@Composable
private fun LiquidChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun StressGraph(
    readings: List<ThermometerEventEntity>,
    liquid: ThermometerLiquid,
) {
    val points = remember(readings) {
        readings
            .sortedWith(compareBy<ThermometerEventEntity> { it.recordedAt }.thenBy { it.id })
            .mapNotNull { event ->
                val score = event.score ?: return@mapNotNull null
                score to event.recordedAt
            }
    }
    if (points.isEmpty()) {
        Text("No graph yet.", style = MaterialTheme.typography.bodySmall)
        return
    }
    val stamps = remember(points) { ThermometerGraphStamps.labels(points.map { it.second }) }
    val line = thermometerMercuryColor(liquid)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp),
    ) {
        val labelBand = 28.dp.toPx()
        val plotHeight = (size.height - labelBand).coerceAtLeast(1f)
        val path = Path()
        fun xAt(index: Int): Float =
            if (points.size == 1) size.width / 2f else size.width * index / (points.size - 1)

        points.forEachIndexed { index, (score, _) ->
            val x = xAt(index)
            val y = plotHeight * (1f - score / ThermometerRules.MAX_SCORE.toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round))
        points.forEachIndexed { index, (score, _) ->
            val x = xAt(index)
            val y = plotHeight * (1f - score / ThermometerRules.MAX_SCORE.toFloat())
            drawCircle(line, radius = 6f, center = Offset(x, y))
        }

        val paint = Paint().apply {
            isAntiAlias = true
            color = labelColor
            textSize = 10.dp.toPx()
            textAlign = Paint.Align.CENTER
        }
        var lastRight = Float.NEGATIVE_INFINITY
        val native = drawContext.canvas.nativeCanvas
        stamps.forEachIndexed { index, label ->
            val width = paint.measureText(label)
            val x = xAt(index)
            val isLast = index == stamps.lastIndex
            val left = x - width / 2f
            if (left <= lastRight + 8f && !isLast) return@forEachIndexed
            if (isLast && left <= lastRight + 8f) {
                paint.textAlign = Paint.Align.RIGHT
                native.drawText(label, size.width, size.height - 4f, paint)
                paint.textAlign = Paint.Align.CENTER
                lastRight = size.width
            } else {
                native.drawText(label, x, size.height - 4f, paint)
                lastRight = x + width / 2f
            }
        }
    }
}

private fun previousScores(readings: List<ThermometerEventEntity>): Map<Long, Int> =
    ThermometerChangeText.previousScores(
        readings.mapNotNull { event ->
            val score = event.score ?: return@mapNotNull null
            ThermometerReadingRef(
                id = event.id,
                thermometerId = event.thermometerId,
                score = score,
                recordedAt = event.recordedAt,
            )
        },
    )

private fun historyLine(event: ThermometerEventEntity, fromScore: Int?): String {
    val whenText = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(event.recordedAt))
    return when (event.type) {
        ThermometerEventType.RENAME -> {
            val from = event.previousName?.ifBlank { "untitled" } ?: "untitled"
            "$whenText · Renamed from $from to ${event.newName.orEmpty()}"
        }
        else -> {
            val detail = ThermometerChangeText.activitySubtitle(
                name = "",
                fromScore = fromScore,
                toScore = event.score ?: 0,
                reason = event.note,
                lowerPlan = event.reliefNote,
            ).removePrefix(" - ").replaceFirst("stress", "Stress")
            "$whenText · $detail"
        }
    }
}
