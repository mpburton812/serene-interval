package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.ui.theme.SereneSpacing
import kotlin.math.roundToInt

@Composable
internal fun ReorderableAffirmationList(
    affirmations: List<AffirmationEntity>,
    onEdit: (AffirmationEntity) -> Unit,
    onArchive: (AffirmationEntity) -> Unit,
    onDelete: (AffirmationEntity) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val itemHeightPx = with(LocalDensity.current) { 120.dp.toPx() }
    var draggedIndex by remember(affirmations) { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
        affirmations.forEachIndexed { index, affirmation ->
            val isDragging = draggedIndex == index
            AffirmationCollectionCard(
                affirmation = affirmation,
                onEdit = { onEdit(affirmation) },
                onArchive = { onArchive(affirmation) },
                onDelete = { onDelete(affirmation) },
                isDragging = isDragging,
                modifier = Modifier
                    .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(affirmations, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                dragStartIndex = index
                                dragOffsetY = 0f
                            },
                            onDragEnd = {
                                val from = dragStartIndex
                                val to = (from + (dragOffsetY / itemHeightPx).roundToInt())
                                    .coerceIn(0, affirmations.lastIndex)
                                if (from in affirmations.indices && from != to) {
                                    onReorder(from, to)
                                }
                                draggedIndex = -1
                                dragStartIndex = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                draggedIndex = -1
                                dragStartIndex = -1
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                            },
                        )
                    },
            )
        }
    }
}
