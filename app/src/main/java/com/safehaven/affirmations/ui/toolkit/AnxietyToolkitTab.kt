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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
private fun ToolDetailScreen(
    tool: ToolkitTool,
    stepIndex: Int,
    currentStep: String?,
    isLastStep: Boolean,
    thoughtDumpText: String,
    draftMoodLevel: Int?,
    anxietyLogText: String,
    thoughtDumpEntries: List<ThoughtDumpEntity>,
    anxietyLogEntries: List<ThoughtDumpEntity>,
    openedLogEntry: ThoughtDumpEntity?,
    futureSelfText: String,
    futureSelfScheduledAtMillis: Long,
    futureSelfEntries: List<FutureSelfMessageEntity>,
    editingFutureSelfId: Long?,
    openedFutureSelfEntry: FutureSelfMessageEntity?,
    refactoringStepIndex: Int,
    refactoringInterpretation: String,
    refactoringActualFacts: String,
    refactoringExplanation1: String,
    refactoringExplanation2: String,
    refactoringExplanation3: String,
    refactoringEntries: List<RefactoringEntryEntity>,
    openedRefactoringEntry: RefactoringEntryEntity?,
    centerOfGravityStepIndex: Int,
    centerOfGravityThoughtsAndFeelings: String,
    centerOfGravityBodyAndNeeds: String,
    centerOfGravityEntries: List<CenterOfGravityEntryEntity>,
    openedCenterOfGravityEntry: CenterOfGravityEntryEntity?,
    nvcStepIndex: Int,
    nvcObservation: String,
    nvcFeeling: String,
    nvcNeed: String,
    nvcRequest: String,
    nvcEntries: List<NvcEntryEntity>,
    openedNvcEntry: NvcEntryEntity?,
    heartsStepIndex: Int,
    heartsSteps: List<String>,
    heartsPersonName: String,
    heartsEntries: List<HeartsEntryEntity>,
    openedHeartsEntry: HeartsEntryEntity?,
    heartsPartnerSummaries: List<HeartsPartnerSummary>,
    onThoughtDumpChange: (String) -> Unit,
    onDraftMoodLevelChange: (Int?) -> Unit,
    onAnxietyLogChange: (String) -> Unit,
    onFutureSelfTextChange: (String) -> Unit,
    onFutureSelfScheduledAtChange: (Long) -> Unit,
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
    onRefactoringInterpretationChange: (String) -> Unit,
    onRefactoringActualFactsChange: (String) -> Unit,
    onRefactoringExplanation1Change: (String) -> Unit,
    onRefactoringExplanation2Change: (String) -> Unit,
    onRefactoringExplanation3Change: (String) -> Unit,
    onSaveRefactoringEntry: () -> Unit,
    onClearRefactoringDraft: () -> Unit,
    onOpenRefactoringEntry: (RefactoringEntryEntity) -> Unit,
    onDeleteRefactoringEntry: (RefactoringEntryEntity) -> Unit,
    onCloseRefactoringEntry: () -> Unit,
    onNextRefactoringStep: () -> Unit,
    onPreviousRefactoringStep: () -> Unit,
    onRefactoringStepChange: (Int) -> Unit,
    onCenterOfGravityThoughtsAndFeelingsChange: (String) -> Unit,
    onCenterOfGravityBodyAndNeedsChange: (String) -> Unit,
    onSaveCenterOfGravityEntry: () -> Unit,
    onClearCenterOfGravityDraft: () -> Unit,
    onOpenCenterOfGravityEntry: (CenterOfGravityEntryEntity) -> Unit,
    onDeleteCenterOfGravityEntry: (CenterOfGravityEntryEntity) -> Unit,
    onCloseCenterOfGravityEntry: () -> Unit,
    onNextCenterOfGravityStep: () -> Unit,
    onPreviousCenterOfGravityStep: () -> Unit,
    onNvcObservationChange: (String) -> Unit,
    onNvcFeelingChange: (String) -> Unit,
    onNvcNeedChange: (String) -> Unit,
    onNvcRequestChange: (String) -> Unit,
    onSaveNvcEntry: () -> Unit,
    onClearNvcDraft: () -> Unit,
    onOpenNvcEntry: (NvcEntryEntity) -> Unit,
    onDeleteNvcEntry: (NvcEntryEntity) -> Unit,
    onCloseNvcEntry: () -> Unit,
    onNextNvcStep: () -> Unit,
    onPreviousNvcStep: () -> Unit,
    onNvcStepChange: (Int) -> Unit,
    onHeartsPersonNameChange: (String) -> Unit,
    onHeartsStepChange: (Int, String) -> Unit,
    onSaveHeartsEntry: () -> Unit,
    onClearHeartsDraft: () -> Unit,
    onOpenHeartsEntry: (HeartsEntryEntity) -> Unit,
    onDeleteHeartsEntry: (HeartsEntryEntity) -> Unit,
    onCloseHeartsEntry: () -> Unit,
    onNextHeartsStep: () -> Unit,
    onPreviousHeartsStep: () -> Unit,
    onGoToHeartsStep: (Int) -> Unit,
    onOpenDelightForPartner: (HeartsPartnerSummary) -> Unit,
    onOpenAttunementForPartner: (HeartsPartnerSummary) -> Unit,
    onOpenRepairForPartner: (HeartsPartnerSummary) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onCompleteFlow: () -> Unit,
    onNavigateToBreathe: () -> Unit,
    showOneNoteSync: Boolean,
    onSyncEntryToOneNote: (OneNoteEntryType, Long) -> Unit,
) {
    val context = LocalContext.current
    val futureSelfSchedulingAvailable = SchedulingPermissions.canScheduleExactAlarms(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 4.dp),
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
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    entries = thoughtDumpEntries,
                    openedEntry = openedLogEntry,
                    onTextChange = onThoughtDumpChange,
                    onSave = onSaveThoughtDump,
                    onClear = onClearDraft,
                    onClose = onClose,
                    onOpenEntry = onOpenLogEntry,
                    onDeleteEntry = onDeleteLogEntry,
                    onCloseEntry = onCloseLogEntry,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.THOUGHT_DUMP, entry.id)
                    },
                )
            }
            ToolkitToolId.AnxietyLog -> {
                ToolkitLogContent(
                    instructionText = "Notice, Observe, and Acknowledge. Feelings are temporary. I am Fine. I am Not Fine. I am Fine.",
                    text = anxietyLogText,
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    entries = anxietyLogEntries,
                    openedEntry = openedLogEntry,
                    onTextChange = onAnxietyLogChange,
                    onSave = onSaveAnxietyLog,
                    onClear = onClearDraft,
                    onClose = onClose,
                    onOpenEntry = onOpenLogEntry,
                    onDeleteEntry = onDeleteLogEntry,
                    onCloseEntry = onCloseLogEntry,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.ANXIETY_LOG, entry.id)
                    },
                )
            }
            ToolkitToolId.FutureSelfMessage -> {
                FutureSelfMessageContent(
                    text = futureSelfText,
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    scheduledAtMillis = futureSelfScheduledAtMillis,
                    entries = futureSelfEntries,
                    editingEntryId = editingFutureSelfId,
                    openedEntry = openedFutureSelfEntry,
                    onTextChange = onFutureSelfTextChange,
                    onScheduledAtChange = onFutureSelfScheduledAtChange,
                    onSave = onSaveFutureSelfMessage,
                    onClear = onClearDraft,
                    onEditEntry = onEditFutureSelfEntry,
                    onDeleteEntry = onDeleteFutureSelfEntry,
                    onOpenEntry = onOpenFutureSelfEntry,
                    onCloseEntry = onCloseFutureSelfEntry,
                    schedulingAvailable = futureSelfSchedulingAvailable,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.FUTURE_SELF, entry.id)
                    },
                )
            }
            ToolkitToolId.Refactoring -> {
                RefactoringContent(
                    stepIndex = refactoringStepIndex,
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    interpretation = refactoringInterpretation,
                    actualFacts = refactoringActualFacts,
                    explanation1 = refactoringExplanation1,
                    explanation2 = refactoringExplanation2,
                    explanation3 = refactoringExplanation3,
                    entries = refactoringEntries,
                    openedEntry = openedRefactoringEntry,
                    onInterpretationChange = onRefactoringInterpretationChange,
                    onActualFactsChange = onRefactoringActualFactsChange,
                    onExplanation1Change = onRefactoringExplanation1Change,
                    onExplanation2Change = onRefactoringExplanation2Change,
                    onExplanation3Change = onRefactoringExplanation3Change,
                    onPrevious = onPreviousRefactoringStep,
                    onNext = onNextRefactoringStep,
                    onStepChange = onRefactoringStepChange,
                    onSave = onSaveRefactoringEntry,
                    onClear = onClearRefactoringDraft,
                    onOpenEntry = onOpenRefactoringEntry,
                    onDeleteEntry = onDeleteRefactoringEntry,
                    onCloseEntry = onCloseRefactoringEntry,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.REFACTORING, entry.id)
                    },
                )
            }
            ToolkitToolId.RelocateCenterOfGravity -> {
                CenterOfGravityContent(
                    stepIndex = centerOfGravityStepIndex,
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    thoughtsAndFeelings = centerOfGravityThoughtsAndFeelings,
                    bodyAndNeeds = centerOfGravityBodyAndNeeds,
                    entries = centerOfGravityEntries,
                    openedEntry = openedCenterOfGravityEntry,
                    onThoughtsAndFeelingsChange = onCenterOfGravityThoughtsAndFeelingsChange,
                    onBodyAndNeedsChange = onCenterOfGravityBodyAndNeedsChange,
                    onPrevious = onPreviousCenterOfGravityStep,
                    onNext = onNextCenterOfGravityStep,
                    onSave = onSaveCenterOfGravityEntry,
                    onClear = onClearCenterOfGravityDraft,
                    onOpenEntry = onOpenCenterOfGravityEntry,
                    onDeleteEntry = onDeleteCenterOfGravityEntry,
                    onCloseEntry = onCloseCenterOfGravityEntry,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.CENTER_OF_GRAVITY, entry.id)
                    },
                )
            }
            ToolkitToolId.NonViolentCommunication -> {
                NvcContent(
                    stepIndex = nvcStepIndex,
                    selectedMoodLevel = draftMoodLevel,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    observation = nvcObservation,
                    feeling = nvcFeeling,
                    need = nvcNeed,
                    request = nvcRequest,
                    entries = nvcEntries,
                    openedEntry = openedNvcEntry,
                    onObservationChange = onNvcObservationChange,
                    onFeelingChange = onNvcFeelingChange,
                    onNeedChange = onNvcNeedChange,
                    onRequestChange = onNvcRequestChange,
                    onPrevious = onPreviousNvcStep,
                    onNext = onNextNvcStep,
                    onStepChange = onNvcStepChange,
                    onSave = onSaveNvcEntry,
                    onClear = onClearNvcDraft,
                    onOpenEntry = onOpenNvcEntry,
                    onDeleteEntry = onDeleteNvcEntry,
                    onCloseEntry = onCloseNvcEntry,
                    showOneNoteSync = showOneNoteSync,
                    onSyncEntryToOneNote = { entry ->
                        onSyncEntryToOneNote(OneNoteEntryType.NVC, entry.id)
                    },
                )
            }
            ToolkitToolId.HeartsFlowerPartners -> {
                HeartsFlowerPartnersContent(
                    partners = heartsPartnerSummaries,
                    onOpenDelightForPartner = onOpenDelightForPartner,
                    onOpenAttunementForPartner = onOpenAttunementForPartner,
                    onOpenRepairForPartner = onOpenRepairForPartner,
                )
            }
            in HeartsToolConfig.journalToolIds() -> {
                HeartsToolkitContent(
                    tool = tool,
                    stepIndex = heartsStepIndex,
                    steps = heartsSteps,
                    personName = heartsPersonName,
                    selectedMoodLevel = draftMoodLevel,
                    entries = heartsEntries,
                    openedEntry = openedHeartsEntry,
                    onPersonNameChange = onHeartsPersonNameChange,
                    onMoodLevelChange = onDraftMoodLevelChange,
                    onStepChange = onHeartsStepChange,
                    onGoToStep = onGoToHeartsStep,
                    onPrevious = onPreviousHeartsStep,
                    onNext = onNextHeartsStep,
                    onSave = onSaveHeartsEntry,
                    onClear = onClearHeartsDraft,
                    onOpenEntry = onOpenHeartsEntry,
                    onDeleteEntry = onDeleteHeartsEntry,
                    onCloseEntry = onCloseHeartsEntry,
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
                        onClick = if (isLastStep) onCompleteFlow else onNext,
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
