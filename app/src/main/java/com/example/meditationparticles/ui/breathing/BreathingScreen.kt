package com.example.meditationparticles.ui.breathing

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.canvas.BreathingAtmosphereBackground
import com.example.meditationparticles.canvas.BreathingCanvas
import com.example.meditationparticles.canvas.BreathingCanvasDisplayMode
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.BreathingVisualMode
import com.example.meditationparticles.domain.breathing.SessionMode
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.SereneHeaderPlate
import com.example.meditationparticles.ui.components.SereneTabBackground
import com.example.meditationparticles.ui.components.SereneTabHeader
import com.example.meditationparticles.ui.theme.SereneSpacing

private val PhaseTextFadeMs = 650
private val CountdownFadeMs = 350
private val ExerciseDissolveMs = 600
private val FabSize = 56.dp
private val FabClearance = 72.dp

@Composable
fun BreathingScreen(
    modifier: Modifier = Modifier,
    pendingPatternId: String? = null,
    onPendingPatternConsumed: () -> Unit = {},
    viewModel: BreathingViewModel = viewModel(),
    onSessionActiveChange: (Boolean) -> Unit = {},
) {
    LaunchedEffect(pendingPatternId) {
        pendingPatternId?.let { patternId ->
            viewModel.selectPattern(BreathingPattern.byId(patternId))
            onPendingPatternConsumed()
        }
    }

    val state by viewModel.sessionState.collectAsState()
    val visualMode by viewModel.visualMode.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    val sessionActive = state.isRunning && state.phase != BreathPhase.Complete
    val exerciseBlend by animateFloatAsState(
        targetValue = if (sessionActive) 1f else 0f,
        animationSpec = tween(ExerciseDissolveMs),
        label = "exercise_dissolve",
    )
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var fabClearancePx by remember { mutableFloatStateOf(0f) }

    val bottomInset = with(density) { (fabClearancePx + 8.dp.toPx()).toDp().coerceAtLeast(FabClearance) }

    var wasRunning by remember { mutableStateOf(false) }
    SideEffect {
        if (state.isRunning && !wasRunning) {
            controlsVisible = false
        } else if (!state.isRunning && wasRunning) {
            controlsVisible = true
        }
        wasRunning = state.isRunning
        onSessionActiveChange(sessionActive)
    }

    DisposableEffect(Unit) {
        onDispose { onSessionActiveChange(false) }
    }

    SereneTabBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = sessionActive,
            ) {
                controlsVisible = !controlsVisible
            },
    ) {
        SereneTabHeader(
            title = "Breath",
            controls = {
                BreathingVisualModeToggle(
                    selected = visualMode,
                    onSelect = viewModel::setVisualMode,
                    enabled = !state.isRunning,
                )
            },
            descriptionContent = if (!state.isRunning) {
                {
                    AnimatedContent(
                        targetState = state.pattern.purpose,
                        transitionSpec = {
                            fadeIn(tween(PhaseTextFadeMs)) togetherWith fadeOut(tween(PhaseTextFadeMs))
                        },
                        label = "pattern_purpose",
                    ) { purpose ->
                        Text(
                            text = purpose,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            } else {
                null
            },
        )

        if (controlsVisible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FloatingActionButton(
                    onClick = {
                        if (!state.isRunning) {
                            onSessionActiveChange(true)
                        }
                        viewModel.toggleRunning()
                    },
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 8.dp)
                        .size(FabSize)
                        .onGloballyPositioned { fabClearancePx = 0f },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = when {
                            state.phase == BreathPhase.Complete -> Icons.Default.PlayArrow
                            state.isRunning -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (state.isRunning) "Pause" else "Start",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BreathingAtmosphereBackground(modifier = Modifier.fillMaxSize())

            if (exerciseBlend < 1f) {
                AnimatedContent(
                    targetState = state.pattern.id,
                    transitionSpec = {
                        fadeIn(tween(PhaseTextFadeMs)) togetherWith fadeOut(tween(PhaseTextFadeMs))
                    },
                    label = "pattern_preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(1f - exerciseBlend),
                ) { patternId ->
                    BreathingCanvas(
                        sessionState = state.copy(pattern = BreathingPattern.byId(patternId)),
                        displayMode = BreathingCanvasDisplayMode.Preview,
                        visualMode = visualMode,
                        modifier = Modifier.fillMaxSize(),
                        topInset = with(density) {
                            if (sessionActive) (headerHeightPx + 12.dp.toPx()).toDp() else 12.dp
                        },
                        bottomInset = bottomInset,
                    )
                }
            }

            if (exerciseBlend > 0f) {
                BreathingCanvas(
                    sessionState = state,
                    displayMode = BreathingCanvasDisplayMode.Exercise,
                    visualMode = visualMode,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(exerciseBlend),
                    topInset = with(density) {
                        if (sessionActive) (headerHeightPx + 12.dp.toPx()).toDp() else 12.dp
                    },
                    bottomInset = bottomInset,
                )
            }

            if (sessionActive) {
                BreathingPhaseHeader(
                    state = state,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .onGloballyPositioned { coords ->
                            headerHeightPx = coords.size.height.toFloat()
                        },
                )
            }

            if (!controlsVisible) {
                FloatingActionButton(
                    onClick = {
                        if (!state.isRunning) {
                            onSessionActiveChange(true)
                        }
                        viewModel.toggleRunning()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(FabSize)
                        .onGloballyPositioned {
                            with(density) {
                                fabClearancePx = FabSize.toPx() + 8.dp.toPx()
                            }
                        },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        imageVector = when {
                            state.phase == BreathPhase.Complete -> Icons.Default.PlayArrow
                            state.isRunning -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = if (state.isRunning) "Pause" else "Start",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(400)) + slideInVertically { it / 2 },
            exit = if (sessionActive) {
                fadeOut(tween(0))
            } else {
                fadeOut(tween(400)) + slideOutVertically { it / 2 }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SereneSpacing.containerMargin)
                    .padding(bottom = SereneSpacing.stackMd),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
            ) {
                Text(
                    text = "BREATHING PATTERN",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.padding(start = 8.dp),
                )
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 12.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BreathingPattern.All.forEach { pattern ->
                            val selected = state.pattern.id == pattern.id
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectPattern(pattern) },
                                label = {
                                    Text(
                                        text = pattern.name,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                enabled = !state.isRunning,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
                ) {
                    SessionStatCard(
                        label = "DURATION",
                        value = "${state.targetMinutes}",
                        unit = "MIN",
                        selected = state.sessionMode == SessionMode.Duration,
                        onClick = {
                            viewModel.setSessionMode(SessionMode.Duration)
                            val presets = listOf(5, 10, 15, 20, 30)
                            val next = presets[(presets.indexOf(state.targetMinutes).coerceAtLeast(0) + 1) % presets.size]
                            viewModel.setTargetMinutes(next)
                        },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f),
                    )
                    SessionStatCard(
                        label = if (state.sessionMode == SessionMode.Repetitions) "TARGET" else "CYCLES",
                        value = "${if (state.sessionMode == SessionMode.Repetitions) state.targetRepetitions else state.cycleCount}",
                        unit = if (state.sessionMode == SessionMode.Repetitions) "REPS" else "DONE",
                        selected = state.sessionMode == SessionMode.Repetitions,
                        onClick = {
                            viewModel.setSessionMode(SessionMode.Repetitions)
                            val presets = listOf(4, 8, 12, 16)
                            val next = presets[(presets.indexOf(state.targetRepetitions).coerceAtLeast(0) + 1) % presets.size]
                            viewModel.setTargetRepetitions(next)
                        },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (sessionActive) {
                    Text(
                        text = "Tap anywhere to hide controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun BreathingVisualModeToggle(
    selected: BreathingVisualMode,
    onSelect: (BreathingVisualMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val selectedColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
    ) {
        BreathingVisualMode.entries.forEachIndexed { index, mode ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .clickable(enabled = enabled) { onSelect(mode) }
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.75f else 0.35f)
                    },
                )
            }
            if (index == 0) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .padding(vertical = 6.dp)
                        .border(0.5.dp, borderColor),
                )
            }
        }
    }
}

@Composable
private fun BreathingPhaseHeader(
    state: BreathingSessionState,
    modifier: Modifier = Modifier,
) {
    SereneHeaderPlate(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SereneSpacing.containerMargin)
            .padding(top = 4.dp, bottom = 8.dp),
        cornerRadius = 20.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        AnimatedContent(
            targetState = state.phase,
            transitionSpec = {
                fadeIn(tween(PhaseTextFadeMs)) togetherWith fadeOut(tween(PhaseTextFadeMs))
            },
            label = "phase_label",
        ) { phase ->
            Text(
                text = state.copy(phase = phase).phaseLabel,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }

        if (state.phaseDurationSeconds > 0f) {
            AnimatedContent(
                targetState = state.secondsRemainingInPhase,
                transitionSpec = {
                    fadeIn(tween(CountdownFadeMs)) togetherWith fadeOut(tween(CountdownFadeMs))
                },
                label = "phase_countdown",
                modifier = Modifier.padding(top = 2.dp),
            ) { seconds ->
                Text(
                    text = "${seconds}s",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        }
    }
}

@Composable
private fun SessionStatCard(
    label: String,
    value: String,
    unit: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        cornerRadius = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
