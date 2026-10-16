package com.safehaven.affirmations.ui.toolkit

import com.safehaven.affirmations.data.local.NvcEntryEntity

internal fun ToolkitUiState.toNvcEntry(): NvcEntryEntity = NvcEntryEntity(
    observation = nvcObservation.trim(),
    feeling = nvcFeeling.trim(),
    need = nvcNeed.trim(),
    request = nvcRequest.trim(),
    moodLevel = draftMoodLevel,
)

internal fun ToolkitUiState.clearedNvcDraft(): ToolkitUiState = copy(
    nvcStepIndex = 0,
    nvcObservation = "",
    nvcFeeling = "",
    nvcNeed = "",
    nvcRequest = "",
)
