package com.safehaven.affirmations.ui.livingtree

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.data.local.LivingTreePersonWithTags
import com.safehaven.affirmations.data.local.LivingTreeTagEntity
import com.safehaven.affirmations.domain.livingtree.LivingTreeFilterLogic
import com.safehaven.affirmations.domain.livingtree.LivingTreeLayout
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.SereneTabBackground
import com.safehaven.affirmations.ui.components.SereneTabHeader
import com.safehaven.affirmations.ui.theme.SereneSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LivingTreeScreen(
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LivingTreeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var displayedPeople by remember { mutableStateOf<List<LivingTreePersonWithTags>>(emptyList()) }
    val nodeAlphas = remember { mutableStateMapOf<Long, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(state.visiblePeople.map { it.person.id }) {
        val next = state.visiblePeople
        val nextIds = next.map { it.person.id }.toSet()
        val prevIds = displayedPeople.map { it.person.id }.toSet()
        if (displayedPeople.isEmpty()) {
            displayedPeople = next
            nextIds.forEach { id ->
                nodeAlphas.getOrPut(id) { Animatable(1f) }.snapTo(1f)
            }
            return@LaunchedEffect
        }
        val exiting = prevIds - nextIds
        val entering = nextIds - prevIds
        val byId = (displayedPeople + next).associateBy { it.person.id }
        displayedPeople = (prevIds + nextIds).mapNotNull { byId[it] }
        exiting.forEach { id ->
            scope.launch {
                nodeAlphas.getOrPut(id) { Animatable(1f) }.animateTo(0f, tween(1000))
            }
        }
        entering.forEach { id ->
            scope.launch {
                val anim = nodeAlphas.getOrPut(id) { Animatable(0f) }
                anim.snapTo(0f)
                anim.animateTo(1f, tween(1000))
            }
        }
        delay(1000)
        displayedPeople = next
        exiting.forEach { nodeAlphas.remove(it) }
    }

    SereneTabBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            SereneTabHeader(
                title = "Living Flower",
                description = "Your people, organized by the roles they play in your life.",
                controls = {
                    IconButton(onClick = onOpenSetup) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Setup",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = SereneSpacing.containerMargin),
            ) {
                val density = LocalDensity.current
                val canvasWidthPx = with(density) { maxWidth.toPx() }
                val canvasHeightPx = with(density) { maxHeight.toPx() }
                val centerX = canvasWidthPx / 2f
                val centerY = canvasHeightPx / 2f
                val peopleForLayout = displayedPeople.ifEmpty { state.visiblePeople }
                val layoutSizing = LivingTreeLayout.computeLayoutSizing(
                    canvasWidth = canvasWidthPx,
                    canvasHeight = canvasHeightPx,
                    peopleCount = peopleForLayout.size.coerceAtLeast(1),
                )
                val animatedNodeRadius by animateFloatAsState(
                    targetValue = layoutSizing.nodeRadius,
                    animationSpec = tween(durationMillis = 320),
                    label = "node_radius",
                )
                val animatedCenterRadius by animateFloatAsState(
                    targetValue = layoutSizing.centerRadius,
                    animationSpec = tween(durationMillis = 320),
                    label = "center_radius",
                )
                val personIds = peopleForLayout.map { it.person.id }
                val positions = LivingTreeLayout.radialPositions(
                    personIds = personIds,
                    centerX = centerX,
                    centerY = centerY,
                    orbitRadiusX = layoutSizing.orbitRadiusX,
                    orbitRadiusY = layoutSizing.orbitRadiusY,
                    nodeRadius = animatedNodeRadius,
                    centerRadius = animatedCenterRadius,
                ).associateBy { it.id }

                val tagColors = state.tags.associate { it.id to it.colorArgb }
                val graphNodes = peopleForLayout.mapNotNull { entry ->
                    val layout = positions[entry.person.id] ?: return@mapNotNull null
                    val personTags = entry.tags.map { it.id }.toSet()
                    val matched = LivingTreeFilterLogic.matchedTagIds(personTags, state.selectedTagIds)
                    LivingTreeGraphNode(
                        id = entry.person.id,
                        name = entry.person.name,
                        x = layout.x,
                        y = layout.y,
                        radius = animatedNodeRadius,
                        bubbleColors = LivingTreeFilterLogic.bubbleColors(
                            personTags,
                            state.selectedTagIds,
                            tagColors,
                        ),
                        isFilteredMatch = matched.isNotEmpty(),
                        alpha = nodeAlphas[entry.person.id]?.value ?: 1f,
                    )
                }

                LivingTreeCanvas(
                    centerLabel = state.centerLabel,
                    nodes = graphNodes,
                    centerRadius = animatedCenterRadius,
                    filterActive = state.filterActive,
                    onPersonTap = { id ->
                        state.allPeople.find { it.person.id == id }?.let(viewModel::selectPerson)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            LivingTreeTagBar(
                tags = state.tags,
                tagCounts = state.tagCounts,
                selectedTagIds = state.selectedTagIds,
                onToggleTag = viewModel::toggleTagFilter,
                onClearFilter = viewModel::clearTagFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SereneSpacing.containerMargin),
            )
        }
    }

    state.selectedPerson?.let { person ->
        ModalBottomSheet(
            onDismissRequest = viewModel::dismissPersonDetail,
            sheetState = sheetState,
        ) {
            PersonDetailSheetContent(
                person = person,
                onEdit = { viewModel.startEditPerson(person) },
                modifier = Modifier.padding(SereneSpacing.containerMargin),
            )
        }
    }

    state.editingPerson?.let { person ->
        LivingTreePersonEditorDialog(
            existing = person,
            tags = state.tags,
            onDismiss = viewModel::dismissPersonEditor,
            onSave = { name, notes, tagIds ->
                viewModel.updatePerson(name, notes, tagIds, person.person.id)
            },
        )
    }
}

@Composable
private fun LivingTreeTagBar(
    tags: List<LivingTreeTagEntity>,
    tagCounts: Map<Long, Int>,
    selectedTagIds: List<Long>,
    onToggleTag: (Long) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagFilterChip(
            label = "All",
            count = null,
            colors = emptyList(),
            selected = selectedTagIds.isEmpty(),
            onClick = onClearFilter,
        )
        tags.forEach { tag ->
            val selected = tag.id in selectedTagIds
            val chipColors = if (selected) {
                listOf(Color(tag.colorArgb))
            } else {
                emptyList()
            }
            TagFilterChip(
                label = tag.name,
                count = tagCounts[tag.id] ?: 0,
                colors = chipColors,
                selected = selected,
                onClick = { onToggleTag(tag.id) },
            )
        }
    }
}

@Composable
private fun TagFilterChip(
    label: String,
    count: Int?,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    GlassCard(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                },
            ),
        cornerRadius = 999.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (colors.isNotEmpty()) {
                SplitColorDot(colors = colors, modifier = Modifier.size(14.dp))
            }
            Text(
                text = if (count != null) "$label ($count)" else label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SplitColorDot(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.clip(CircleShape),
    ) {
        when {
            colors.size <= 1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.firstOrNull() ?: Color.White.copy(alpha = 0.3f)),
                )
            }
            colors.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(colors[0]),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(colors[1]),
                    )
                }
            }
            else -> {
                val sweep = 360f / colors.size.coerceAtMost(4)
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    colors.take(4).forEachIndexed { index, color ->
                        drawArc(
                            color = color,
                            startAngle = -90f + index * sweep,
                            sweepAngle = sweep,
                            useCenter = true,
                            size = size,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonDetailSheetContent(
    person: LivingTreePersonWithTags,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = person.person.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEdit) {
                Text("Edit")
            }
        }
        if (person.tags.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                person.tags.forEach { tag ->
                    TagFilterChip(
                        label = tag.name,
                        count = null,
                        colors = listOf(Color(tag.colorArgb)),
                        selected = false,
                        onClick = {},
                    )
                }
            }
        }
        if (person.person.notes.isNotBlank()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = person.person.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            Text(
                text = "No notes yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
