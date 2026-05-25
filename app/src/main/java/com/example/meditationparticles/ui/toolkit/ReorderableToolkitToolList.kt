package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.ui.theme.SereneSpacing
import kotlin.math.roundToInt

@Composable
internal fun ReorderableToolkitToolList(
    tools: List<ToolkitTool>,
    accentBorder: Boolean,
    onToolClick: (ToolkitTool) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    toolCard: @Composable (
        tool: ToolkitTool,
        accentBorder: Boolean,
        onClick: () -> Unit,
        modifier: Modifier,
        isDragging: Boolean,
    ) -> Unit,
) {
    val itemHeightPx = with(LocalDensity.current) { 88.dp.toPx() }
    var draggedIndex by remember(tools) { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }

    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
        tools.forEachIndexed { index, tool ->
            val isDragging = draggedIndex == index
            toolCard(
                tool,
                accentBorder,
                { onToolClick(tool) },
                Modifier
                    .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                    .zIndex(if (isDragging) 1f else 0f)
                    .pointerInput(tools, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                dragStartIndex = index
                                dragOffsetY = 0f
                            },
                            onDragEnd = {
                                val from = dragStartIndex
                                val to = (from + (dragOffsetY / itemHeightPx).roundToInt())
                                    .coerceIn(0, tools.lastIndex)
                                if (from in tools.indices && from != to) {
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
                isDragging,
            )
        }
    }
}
