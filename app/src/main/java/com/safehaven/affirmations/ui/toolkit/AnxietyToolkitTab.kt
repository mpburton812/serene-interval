package com.safehaven.affirmations.ui.toolkit

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import com.safehaven.affirmations.BuildConfig
import com.safehaven.affirmations.data.local.HeartsEntryEntity
import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import com.safehaven.affirmations.domain.toolkit.ToolkitCategory
import com.safehaven.affirmations.domain.toolkit.ToolkitLane
import com.safehaven.affirmations.domain.toolkit.ToolkitTool
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
import com.safehaven.affirmations.domain.toolkit.HeartsToolConfig
import com.safehaven.affirmations.navigation.PendingToolkitNavigation
import com.safehaven.affirmations.permissions.SchedulingPermissions
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.theme.SerenePrimaryContainer
import com.safehaven.affirmations.ui.theme.SereneSecondaryContainer
import com.safehaven.affirmations.ui.theme.SereneSpacing
import com.safehaven.affirmations.ui.theme.SereneTertiary
import com.safehaven.affirmations.ui.theme.SereneTertiaryContainer

@Composable
fun AnxietyToolkitTab(
    onNavigateToBreathe: () -> Unit,
    pendingNavigation: PendingToolkitNavigation? = null,
    returnToHomeOnComplete: Boolean = false,
    onPendingNavigationConsumed: () -> Unit = {},
    onReturnToHome: () -> Unit = {},
    viewModel: ToolkitViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    val completeToolFlow: () -> Unit = {
        viewModel.closeTool()
        if (returnToHomeOnComplete) {
            onReturnToHome()
        }
    }

    LaunchedEffect(pendingNavigation) {
        pendingNavigation?.let { navigation ->
            if (navigation.toolId != null || navigation.futureSelfMessageId != null) {
                viewModel.handlePendingNavigation(
                    toolId = navigation.toolId,
                    futureSelfMessageId = navigation.futureSelfMessageId,
                )
                onPendingNavigationConsumed()
            }
        }
    }

    if (state.selectedTool != null) {
        ToolDetailScreen(
            tool = state.selectedTool!!,
            stepIndex = state.stepIndex,
            currentStep = state.currentStep,
            isLastStep = state.isLastStep,
            thoughtDumpText = state.thoughtDumpText,
            draftMoodLevel = state.draftMoodLevel,
            anxietyLogText = state.anxietyLogText,
            thoughtDumpEntries = state.thoughtDumpEntries,
            anxietyLogEntries = state.anxietyLogEntries,
            openedLogEntry = state.openedLogEntry,
            futureSelfText = state.futureSelfText,
            futureSelfScheduledAtMillis = state.futureSelfScheduledAtMillis,
            futureSelfEntries = state.futureSelfEntries,
            editingFutureSelfId = state.editingFutureSelfId,
            openedFutureSelfEntry = state.openedFutureSelfEntry,
            refactoringStepIndex = state.refactoringStepIndex,
            refactoringInterpretation = state.refactoringInterpretation,
            refactoringActualFacts = state.refactoringActualFacts,
            refactoringExplanation1 = state.refactoringExplanation1,
            refactoringExplanation2 = state.refactoringExplanation2,
            refactoringExplanation3 = state.refactoringExplanation3,
            refactoringEntries = state.refactoringEntries,
            openedRefactoringEntry = state.openedRefactoringEntry,
            centerOfGravityStepIndex = state.centerOfGravityStepIndex,
            centerOfGravityThoughtsAndFeelings = state.centerOfGravityThoughtsAndFeelings,
            centerOfGravityBodyAndNeeds = state.centerOfGravityBodyAndNeeds,
            centerOfGravityEntries = state.centerOfGravityEntries,
            openedCenterOfGravityEntry = state.openedCenterOfGravityEntry,
            nvcStepIndex = state.nvcStepIndex,
            nvcObservation = state.nvcObservation,
            nvcFeeling = state.nvcFeeling,
            nvcNeed = state.nvcNeed,
            nvcRequest = state.nvcRequest,
            nvcEntries = state.nvcEntries,
            openedNvcEntry = state.openedNvcEntry,
            heartsStepIndex = state.heartsStepIndex,
            heartsSteps = state.heartsSteps,
            heartsPersonName = state.heartsPersonName,
            heartsEntries = state.heartsEntries,
            openedHeartsEntry = state.openedHeartsEntry,
            heartsPartnerSummaries = state.heartsPartnerSummaries,
            onThoughtDumpChange = viewModel::updateThoughtDump,
            onDraftMoodLevelChange = viewModel::updateDraftMoodLevel,
            onAnxietyLogChange = viewModel::updateAnxietyLog,
            onFutureSelfTextChange = viewModel::updateFutureSelfText,
            onFutureSelfScheduledAtChange = viewModel::updateFutureSelfScheduledAt,
            onSaveThoughtDump = {
                viewModel.saveThoughtDump(onComplete = completeToolFlow)
            },
            onSaveAnxietyLog = {
                viewModel.saveAnxietyLog(onComplete = completeToolFlow)
            },
            onSaveFutureSelfMessage = {
                viewModel.saveFutureSelfMessage(onComplete = completeToolFlow)
            },
            onClearDraft = viewModel::clearActiveDraft,
            onOpenLogEntry = viewModel::openLogEntry,
            onDeleteLogEntry = viewModel::deleteLogEntry,
            onCloseLogEntry = viewModel::closeLogEntry,
            onEditFutureSelfEntry = viewModel::editFutureSelfEntry,
            onDeleteFutureSelfEntry = viewModel::deleteFutureSelfEntry,
            onOpenFutureSelfEntry = viewModel::openFutureSelfEntry,
            onCloseFutureSelfEntry = viewModel::closeFutureSelfEntry,
            onRefactoringInterpretationChange = viewModel::updateRefactoringInterpretation,
            onRefactoringActualFactsChange = viewModel::updateRefactoringActualFacts,
            onRefactoringExplanation1Change = viewModel::updateRefactoringExplanation1,
            onRefactoringExplanation2Change = viewModel::updateRefactoringExplanation2,
            onRefactoringExplanation3Change = viewModel::updateRefactoringExplanation3,
            onSaveRefactoringEntry = {
                viewModel.saveRefactoringEntry(onComplete = completeToolFlow)
            },
            onClearRefactoringDraft = viewModel::clearRefactoringDraft,
            onOpenRefactoringEntry = viewModel::openRefactoringEntry,
            onDeleteRefactoringEntry = viewModel::deleteRefactoringEntry,
            onCloseRefactoringEntry = viewModel::closeRefactoringEntry,
            onNextRefactoringStep = viewModel::nextRefactoringStep,
            onPreviousRefactoringStep = viewModel::previousRefactoringStep,
            onRefactoringStepChange = viewModel::goToRefactoringStep,
            onCenterOfGravityThoughtsAndFeelingsChange = viewModel::updateCenterOfGravityThoughtsAndFeelings,
            onCenterOfGravityBodyAndNeedsChange = viewModel::updateCenterOfGravityBodyAndNeeds,
            onSaveCenterOfGravityEntry = {
                viewModel.saveCenterOfGravityEntry(onComplete = completeToolFlow)
            },
            onClearCenterOfGravityDraft = viewModel::clearCenterOfGravityDraft,
            onOpenCenterOfGravityEntry = viewModel::openCenterOfGravityEntry,
            onDeleteCenterOfGravityEntry = viewModel::deleteCenterOfGravityEntry,
            onCloseCenterOfGravityEntry = viewModel::closeCenterOfGravityEntry,
            onNextCenterOfGravityStep = viewModel::nextCenterOfGravityStep,
            onPreviousCenterOfGravityStep = viewModel::previousCenterOfGravityStep,
            onNvcObservationChange = viewModel::updateNvcObservation,
            onNvcFeelingChange = viewModel::updateNvcFeeling,
            onNvcNeedChange = viewModel::updateNvcNeed,
            onNvcRequestChange = viewModel::updateNvcRequest,
            onSaveNvcEntry = {
                viewModel.saveNvcEntry(onComplete = completeToolFlow)
            },
            onClearNvcDraft = viewModel::clearNvcDraft,
            onOpenNvcEntry = viewModel::openNvcEntry,
            onDeleteNvcEntry = viewModel::deleteNvcEntry,
            onCloseNvcEntry = viewModel::closeNvcEntry,
            onNextNvcStep = viewModel::nextNvcStep,
            onPreviousNvcStep = viewModel::previousNvcStep,
            onNvcStepChange = viewModel::goToNvcStep,
            onHeartsPersonNameChange = viewModel::updateHeartsPersonName,
            onHeartsStepChange = viewModel::updateHeartsStep,
            onSaveHeartsEntry = { viewModel.saveHeartsEntry(onComplete = completeToolFlow) },
            onClearHeartsDraft = viewModel::clearHeartsDraft,
            onOpenHeartsEntry = viewModel::openHeartsEntry,
            onDeleteHeartsEntry = viewModel::deleteHeartsEntry,
            onCloseHeartsEntry = viewModel::closeHeartsEntry,
            onNextHeartsStep = viewModel::nextHeartsStep,
            onPreviousHeartsStep = viewModel::previousHeartsStep,
            onGoToHeartsStep = viewModel::goToHeartsStep,
            onOpenDelightForPartner = { partner ->
                viewModel.openHeartsToolForPartner(ToolkitToolId.DelightDeposit, partner)
            },
            onOpenAttunementForPartner = { partner ->
                viewModel.openHeartsToolForPartner(ToolkitToolId.AttunementMap, partner)
            },
            onOpenRepairForPartner = { partner ->
                viewModel.openHeartsToolForPartner(ToolkitToolId.RepairReconnect, partner)
            },
            onNext = viewModel::nextStep,
            onPrevious = viewModel::previousStep,
            onClose = viewModel::closeTool,
            onCompleteFlow = completeToolFlow,
            onNavigateToBreathe = onNavigateToBreathe,
            showOneNoteSync = BuildConfig.ONENOTE_SYNC_AVAILABLE && state.oneNoteConnected,
            onSyncEntryToOneNote = viewModel::syncEntryToOneNote,
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

        ToolkitSection(
            title = "HEARTS · Proactive",
            icon = Icons.Default.Favorite,
            titleColor = MaterialTheme.colorScheme.primary,
            tools = state.heartsProactiveTools,
            onToolClick = viewModel::openTool,
            onReorder = viewModel::reorderHeartsProactiveTool,
        )

        ToolkitSection(
            title = "HEARTS · Reactive",
            icon = Icons.Default.Healing,
            titleColor = SereneTertiary,
            tools = state.heartsReactiveTools,
            onToolClick = viewModel::openTool,
            onReorder = viewModel::reorderHeartsReactiveTool,
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
private fun toolIcon(id: ToolkitToolId): ImageVector = when (id) {
    ToolkitToolId.ThoughtDump -> Icons.Default.EditNote
    ToolkitToolId.BoundarySetting -> Icons.Default.NotificationsActive
    ToolkitToolId.MicroPause -> Icons.Default.Schedule
    ToolkitToolId.FutureSelfMessage -> Icons.Default.Mail
    ToolkitToolId.Grounding54321 -> Icons.Default.GridView
    ToolkitToolId.MuscleRelaxation -> Icons.Default.Spa
    ToolkitToolId.LovingKindness -> Icons.Default.Favorite
    ToolkitToolId.AnxietyLog -> Icons.Default.EditNote
    ToolkitToolId.Refactoring -> Icons.Default.Psychology
    ToolkitToolId.NonViolentCommunication -> Icons.Default.Forum
    ToolkitToolId.RelocateCenterOfGravity -> Icons.Default.MyLocation
    ToolkitToolId.DelightDeposit -> Icons.Default.Star
    ToolkitToolId.AttunementMap -> Icons.Default.RecordVoiceOver
    ToolkitToolId.RepairReconnect -> Icons.Default.Handshake
    ToolkitToolId.SecureSelfCheck -> Icons.Default.SelfImprovement
    ToolkitToolId.PresenceTimer -> Icons.Default.Timer
    ToolkitToolId.AppreciationRitual -> Icons.Default.Favorite
    ToolkitToolId.NeedsBeforeNegotiation -> Icons.Default.Psychology
    ToolkitToolId.AttachmentStorySnapshot -> Icons.Default.EditNote
    ToolkitToolId.HeartsFlowerPartners -> Icons.Default.LocalFlorist
}

@Composable
private fun toolIconBackground(tool: ToolkitTool): androidx.compose.ui.graphics.Color =
    when (tool.lane) {
        ToolkitLane.Hearts -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ToolkitLane.Core -> when (tool.category) {
            ToolkitCategory.Proactive -> when (tool.id) {
                ToolkitToolId.BoundarySetting -> SerenePrimaryContainer.copy(alpha = 0.2f)
                else -> SereneSecondaryContainer.copy(alpha = 0.45f)
            }
            ToolkitCategory.Reactive -> SereneTertiaryContainer.copy(alpha = 0.25f)
        }
    }

@Composable
private fun toolIconTint(tool: ToolkitTool): androidx.compose.ui.graphics.Color =
    when (tool.lane) {
        ToolkitLane.Hearts -> MaterialTheme.colorScheme.primary
        ToolkitLane.Core -> when (tool.category) {
            ToolkitCategory.Proactive -> when (tool.id) {
                ToolkitToolId.BoundarySetting -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.secondary
            }
            ToolkitCategory.Reactive -> SereneTertiary
        }
    }
