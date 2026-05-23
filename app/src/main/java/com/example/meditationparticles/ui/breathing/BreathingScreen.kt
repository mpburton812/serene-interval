package com.example.meditationparticles.ui.breathing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.canvas.BreathingCanvas
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.SessionMode
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing

private val PhaseTextFadeMs = 650
private val CountdownFadeMs = 350
private val FabSize = 56.dp
private val FabClearance = 72.dp

@Composable
fun BreathingScreen(
    modifier: Modifier = Modifier,
    viewModel: BreathingViewModel = viewModel(),
) {
    val state by viewModel.sessionState.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    val immersive = state.isRunning && state.phase != BreathPhase.Complete
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var fabClearancePx by remember { mutableFloatStateOf(0f) }

    val bottomInset = with(density) { (fabClearancePx + 8.dp.toPx()).toDp().coerceAtLeast(FabClearance) }

    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            kotlinx.coroutines.delay(2_500)
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = immersive,
            ) {
                controlsVisible = !controlsVisible
            },
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            BreathingCanvas(
                sessionState = state,
                modifier = Modifier.fillMaxSize(),
                topInset = with(density) { (headerHeightPx + 12.dp.toPx()).toDp() },
                bottomInset = bottomInset,
            )

            BreathingPhaseHeader(
                state = state,
                compact = immersive && !controlsVisible,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { coords ->
                        headerHeightPx = coords.size.height.toFloat()
                    },
            )

            FloatingActionButton(
                onClick = { viewModel.toggleRunning() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(FabSize)
                    .onGloballyPositioned {
                        with(density) {
                            fabClearancePx = FabSize.toPx() + 16.dp.toPx()
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

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(400)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(400)) + slideOutVertically { it / 2 },
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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

                if (immersive) {
                    Text(
                        text = "Tap anywhere to hide controls",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun BreathingPhaseHeader(
    state: BreathingSessionState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = if (compact) 0.72f else 0.88f))
            .padding(horizontal = SereneSpacing.containerMargin)
            .padding(top = 4.dp, bottom = if (compact) 4.dp else 8.dp),
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

        if (state.isRunning && state.phaseDurationSeconds > 0f) {
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (!compact) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    fadeIn(tween(PhaseTextFadeMs)) togetherWith fadeOut(tween(PhaseTextFadeMs))
                },
                label = "phase_description",
                modifier = Modifier.padding(top = 4.dp),
            ) { phase ->
                Text(
                    text = state.copy(phase = phase).phaseDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }

            if (!state.isRunning) {
                AnimatedContent(
                    targetState = state.pattern.purpose,
                    transitionSpec = {
                        fadeIn(tween(PhaseTextFadeMs)) togetherWith fadeOut(tween(PhaseTextFadeMs))
                    },
                    label = "pattern_purpose",
                    modifier = Modifier.padding(top = SereneSpacing.stackSm),
                ) { purpose ->
                    Text(
                        text = purpose,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
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
