package com.safehaven.affirmations.ui.toolkit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.data.local.CenterOfGravityEntryEntity
import com.safehaven.affirmations.data.local.FutureSelfMessageEntity
import com.safehaven.affirmations.data.local.HeartsEntryEntity
import com.safehaven.affirmations.data.local.NvcEntryEntity
import com.safehaven.affirmations.data.local.RefactoringEntryEntity
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import com.safehaven.affirmations.domain.toolkit.HeartsToolConfig
import com.safehaven.affirmations.domain.toolkit.ToolkitTool
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
import com.safehaven.affirmations.permissions.SchedulingPermissions
import com.safehaven.affirmations.ui.components.GlassCard
import androidx.compose.ui.res.stringResource
import com.safehaven.affirmations.R
import com.safehaven.affirmations.ui.theme.SereneSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ToolDetailScreen(
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
    val isExpandableJournalCapture =
        tool.id == ToolkitToolId.ThoughtDump || tool.id == ToolkitToolId.AnxietyLog
    val imeVisible = WindowInsets.isImeVisible

    Column(
        modifier = if (isExpandableJournalCapture) {
            // Scaffold already insets for the IME; extra imePadding collapses the field.
            Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        } else {
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp)
        },
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        TextButton(onClick = onClose) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back_to_toolkit),
            )
            if (!isExpandableJournalCapture || !imeVisible) {
                Text("Back to Toolkit", modifier = Modifier.padding(start = 4.dp))
            }
        }

        if (!isExpandableJournalCapture || !imeVisible) {
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
        }

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
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fillAvailableHeight = true,
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
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    fillAvailableHeight = true,
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
