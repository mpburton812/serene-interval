package com.example.meditationparticles.ui.toolkit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.permissions.SchedulingPermissions
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SerenePrimaryContainer
import com.example.meditationparticles.ui.theme.SereneSecondaryContainer
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.theme.SereneTertiary
import com.example.meditationparticles.ui.theme.SereneTertiaryContainer

@Composable
fun AnxietyToolkitTab(
    onNavigateToBreathe: () -> Unit,
    pendingNavigation: PendingToolkitNavigation? = null,
    viewModel: ToolkitViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { navigation ->
            viewModel.handlePendingNavigation(
                toolId = navigation.toolId,
                futureSelfMessageId = navigation.futureSelfMessageId,
            )
        }
    }

    if (state.selectedTool != null) {
        ToolDetailScreen(
            tool = state.selectedTool!!,
            stepIndex = state.stepIndex,
            currentStep = state.currentStep,
            isLastStep = state.isLastStep,
            thoughtDumpText = state.thoughtDumpText,
            anxietyLogText = state.anxietyLogText,
            pendingAudioPath = state.pendingAudioPath,
            thoughtDumpEntries = state.thoughtDumpEntries,
            anxietyLogEntries = state.anxietyLogEntries,
            openedLogEntry = state.openedLogEntry,
            futureSelfText = state.futureSelfText,
            futureSelfScheduledAtMillis = state.futureSelfScheduledAtMillis,
            futureSelfEntries = state.futureSelfEntries,
            editingFutureSelfId = state.editingFutureSelfId,
            openedFutureSelfEntry = state.openedFutureSelfEntry,
            onThoughtDumpChange = viewModel::updateThoughtDump,
            onAnxietyLogChange = viewModel::updateAnxietyLog,
            onFutureSelfTextChange = viewModel::updateFutureSelfText,
            onFutureSelfScheduledAtChange = viewModel::updateFutureSelfScheduledAt,
            onPendingAudioChange = viewModel::setPendingAudioPath,
            onSpeechResult = viewModel::appendToActiveLog,
            onSaveThoughtDump = viewModel::saveThoughtDump,
            onSaveAnxietyLog = viewModel::saveAnxietyLog,
            onSaveFutureSelfMessage = viewModel::saveFutureSelfMessage,
            onClearDraft = viewModel::clearActiveDraft,
            onOpenLogEntry = viewModel::openLogEntry,
            onDeleteLogEntry = viewModel::deleteLogEntry,
            onCloseLogEntry = viewModel::closeLogEntry,
            onEditFutureSelfEntry = viewModel::editFutureSelfEntry,
            onDeleteFutureSelfEntry = viewModel::deleteFutureSelfEntry,
            onOpenFutureSelfEntry = viewModel::openFutureSelfEntry,
            onCloseFutureSelfEntry = viewModel::closeFutureSelfEntry,
            onNext = viewModel::nextStep,
            onPrevious = viewModel::previousStep,
            onClose = viewModel::closeTool,
            onNavigateToBreathe = onNavigateToBreathe,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        RandomReliefCard(
            state = state.randomToolState,
            selectedTool = state.randomSelectedTool,
            onPickRandom = viewModel::pickRandomTool,
        )

        ToolkitSection(
            title = "Proactive Care",
            icon = Icons.Default.Shield,
            titleColor = MaterialTheme.colorScheme.primary,
            tools = state.proactiveTools,
            onToolClick = viewModel::openTool,
            onReorder = viewModel::reorderProactiveTool,
        )

        ToolkitSection(
            title = "Reactive Relief",
            icon = Icons.Default.Emergency,
            titleColor = SereneTertiary,
            tools = state.reactiveTools,
            onToolClick = viewModel::openTool,
            onReorder = viewModel::reorderReactiveTool,
            accentBorder = true,
        )
    }
}

@Composable
private fun RandomReliefCard(
    state: RandomToolState,
    selectedTool: ToolkitTool?,
    onPickRandom: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 40.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SereneTertiaryContainer.copy(alpha = 0.08f))
                .border(
                    width = 2.dp,
                    color = SereneTertiaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(40.dp),
                )
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Text(
                text = "Feeling Overwhelmed?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Let us pick a grounding technique for you right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onPickRandom,
                enabled = state == RandomToolState.Idle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                when (state) {
                    RandomToolState.Idle -> {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Text("Get Random Tool", modifier = Modifier.padding(start = 8.dp))
                    }
                    RandomToolState.Finding -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                        Text("Finding…", modifier = Modifier.padding(start = 8.dp))
                    }
                    RandomToolState.Selected -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Text(
                            selectedTool?.title ?: "Selected",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolkitSection(
    title: String,
    icon: ImageVector,
    titleColor: androidx.compose.ui.graphics.Color,
    tools: List<ToolkitTool>,
    onToolClick: (ToolkitTool) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    accentBorder: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = titleColor)
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = titleColor)
        }

        Text(
            text = "Press and hold to reorder",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ReorderableToolkitToolList(
            tools = tools,
            accentBorder = accentBorder,
            onToolClick = onToolClick,
            onReorder = onReorder,
        ) { tool, sectionAccentBorder, onClick, itemModifier, isDragging ->
            ToolkitToolCard(
                tool = tool,
                accentBorder = sectionAccentBorder,
                onClick = onClick,
                isDragging = isDragging,
                modifier = itemModifier,
            )
        }
    }
}

@Composable
private fun ToolkitToolCard(
    tool: ToolkitTool,
    accentBorder: Boolean,
    onClick: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isDragging) 0.92f else 1f)
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (accentBorder) {
                        Modifier.border(
                            width = 0.dp,
                            color = androidx.compose.ui.graphics.Color.Transparent,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (accentBorder) {
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SereneTertiaryContainer),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(toolIconBackground(tool)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = toolIcon(tool.id),
                    contentDescription = null,
                    tint = toolIconTint(tool),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = tool.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )

            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ToolDetailScreen(
    tool: ToolkitTool,
    stepIndex: Int,
    currentStep: String?,
    isLastStep: Boolean,
    thoughtDumpText: String,
    anxietyLogText: String,
    pendingAudioPath: String?,
    thoughtDumpEntries: List<ThoughtDumpEntity>,
    anxietyLogEntries: List<ThoughtDumpEntity>,
    openedLogEntry: ThoughtDumpEntity?,
    futureSelfText: String,
    futureSelfScheduledAtMillis: Long,
    futureSelfEntries: List<FutureSelfMessageEntity>,
    editingFutureSelfId: Long?,
    openedFutureSelfEntry: FutureSelfMessageEntity?,
    onThoughtDumpChange: (String) -> Unit,
    onAnxietyLogChange: (String) -> Unit,
    onFutureSelfTextChange: (String) -> Unit,
    onFutureSelfScheduledAtChange: (Long) -> Unit,
    onPendingAudioChange: (String?) -> Unit,
    onSpeechResult: (String) -> Unit,
    onSaveThoughtDump: () -> Unit,
    onSaveAnxietyLog: () -> Unit,
    onSaveFutureSelfMessage: () -> Unit,
    onClearDraft: () -> Unit,
    onOpenLogEntry: (ThoughtDumpEntity) -> Unit,
    onDeleteLogEntry: (ThoughtDumpEntity) -> Unit,
    onCloseLogEntry: () -> Unit,
    onEditFutureSelfEntry: (FutureSelfMessageEntity) -> Unit,
    onDeleteFutureSelfEntry: (FutureSelfMessageEntity) -> Unit,
    onOpenFutureSelfEntry: (FutureSelfMessageEntity) -> Unit,
    onCloseFutureSelfEntry: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onNavigateToBreathe: () -> Unit,
) {
    val context = LocalContext.current
    val futureSelfSchedulingAvailable = SchedulingPermissions.canScheduleExactAlarms(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        TextButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text("Back to Toolkit", modifier = Modifier.padding(start = 4.dp))
        }

        Text(
            text = tool.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (tool.id) {
            ToolkitToolId.ThoughtDump -> {
                ToolkitLogContent(
                    instructionText = "Write everything on your mind. No editing, no judgment.",
                    text = thoughtDumpText,
                    entries = thoughtDumpEntries,
                    pendingAudioPath = pendingAudioPath,
                    openedEntry = openedLogEntry,
                    onTextChange = onThoughtDumpChange,
                    onPendingAudioChange = onPendingAudioChange,
                    onSpeechResult = onSpeechResult,
                    onSave = onSaveThoughtDump,
                    onClear = onClearDraft,
                    onClose = onClose,
                    onOpenEntry = onOpenLogEntry,
                    onDeleteEntry = onDeleteLogEntry,
                    onCloseEntry = onCloseLogEntry,
                )
            }
            ToolkitToolId.AnxietyLog -> {
                ToolkitLogContent(
                    instructionText = "Notice, Observe, and Acknowledge. Feelings are temporary. I am Fine. I am Not Fine. I am Fine.",
                    text = anxietyLogText,
                    entries = anxietyLogEntries,
                    pendingAudioPath = pendingAudioPath,
                    openedEntry = openedLogEntry,
                    onTextChange = onAnxietyLogChange,
                    onPendingAudioChange = onPendingAudioChange,
                    onSpeechResult = onSpeechResult,
                    onSave = onSaveAnxietyLog,
                    onClear = onClearDraft,
                    onClose = onClose,
                    onOpenEntry = onOpenLogEntry,
                    onDeleteEntry = onDeleteLogEntry,
                    onCloseEntry = onCloseLogEntry,
                )
            }
            ToolkitToolId.FutureSelfMessage -> {
                FutureSelfMessageContent(
                    text = futureSelfText,
                    scheduledAtMillis = futureSelfScheduledAtMillis,
                    pendingAudioPath = pendingAudioPath,
                    entries = futureSelfEntries,
                    editingEntryId = editingFutureSelfId,
                    openedEntry = openedFutureSelfEntry,
                    onTextChange = onFutureSelfTextChange,
                    onScheduledAtChange = onFutureSelfScheduledAtChange,
                    onPendingAudioChange = onPendingAudioChange,
                    onSpeechResult = onSpeechResult,
                    onSave = onSaveFutureSelfMessage,
                    onClear = onClearDraft,
                    onEditEntry = onEditFutureSelfEntry,
                    onDeleteEntry = onDeleteFutureSelfEntry,
                    onOpenEntry = onOpenFutureSelfEntry,
                    onCloseEntry = onCloseFutureSelfEntry,
                    schedulingAvailable = futureSelfSchedulingAvailable,
                )
            }
            else -> {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
                    ) {
                        Text(
                            text = "Step ${stepIndex + 1} of ${tool.steps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        AnimatedContent(
                            targetState = currentStep ?: "",
                            transitionSpec = {
                                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                            },
                            label = "tool_step",
                        ) { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
                ) {
                    OutlinedButton(
                        onClick = onPrevious,
                        enabled = stepIndex > 0,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Previous")
                    }

                    Button(
                        onClick = if (isLastStep) onClose else onNext,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (isLastStep) "Done" else "Next")
                        if (!isLastStep) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun toolIcon(id: ToolkitToolId): ImageVector = when (id) {
    ToolkitToolId.ThoughtDump -> Icons.Default.EditNote
    ToolkitToolId.BoundarySetting -> Icons.Default.NotificationsActive
    ToolkitToolId.MicroPause -> Icons.Default.Schedule
    ToolkitToolId.FutureSelfMessage -> Icons.Default.Mail
    ToolkitToolId.Grounding54321 -> Icons.Default.GridView
    ToolkitToolId.MuscleRelaxation -> Icons.Default.Spa
    ToolkitToolId.LovingKindness -> Icons.Default.Favorite
    ToolkitToolId.AnxietyLog -> Icons.Default.EditNote
}

@Composable
private fun toolIconBackground(tool: ToolkitTool): androidx.compose.ui.graphics.Color =
    when (tool.category) {
        ToolkitCategory.Proactive -> when (tool.id) {
            ToolkitToolId.BoundarySetting -> SerenePrimaryContainer.copy(alpha = 0.2f)
            else -> SereneSecondaryContainer.copy(alpha = 0.45f)
        }
        ToolkitCategory.Reactive -> SereneTertiaryContainer.copy(alpha = 0.25f)
    }

@Composable
private fun toolIconTint(tool: ToolkitTool): androidx.compose.ui.graphics.Color =
    when (tool.category) {
        ToolkitCategory.Proactive -> when (tool.id) {
            ToolkitToolId.BoundarySetting -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        }
        ToolkitCategory.Reactive -> SereneTertiary
    }
