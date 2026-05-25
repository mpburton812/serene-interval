package com.example.meditationparticles.ui.timer

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.audio.TimerAudioPlayer
import com.example.meditationparticles.canvas.HourglassCanvas
import com.example.meditationparticles.data.TimerPreferences
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerPhase
import com.example.meditationparticles.domain.timer.TimerPrepareTiming
import com.example.meditationparticles.domain.timer.TimerSessionState
import com.example.meditationparticles.domain.timer.TimerSoundOption
import com.example.meditationparticles.reminder.MeditationReminderScheduler
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing

private val TextFadeMs = 650
private val FabSize = 56.dp
private val FabClearance = 72.dp

@Composable
fun TimerScreen(
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = viewModel(),
) {
    val state by viewModel.sessionState.collectAsState()
    val context = LocalContext.current
    val preferences = remember { TimerPreferences(context) }
    val audioPlayer = remember { TimerAudioPlayer(context) }
    var controlsVisible by remember { mutableStateOf(true) }
    val immersive = state.isRunning && state.phase != TimerPhase.Complete
    val showStatusHeader = state.phase == TimerPhase.Idle ||
        state.phase == TimerPhase.Complete ||
        (controlsVisible && state.phase != TimerPhase.Prepare)
    val showDisplayCountdown = state.phase == TimerPhase.Running &&
        (state.displayMode == TimerDisplayMode.Digital ||
            (state.displayMode == TimerDisplayMode.Blank && controlsVisible && !immersive))

    LaunchedEffect(Unit) {
        val saved = preferences.load()
        viewModel.restorePreferences(
            displayMode = saved.displayMode,
            targetMinutes = saved.targetMinutes,
            sound = saved.sound,
            customSoundUri = saved.customSoundUri,
            reminderEnabled = saved.reminderEnabled,
            reminderHour = saved.reminderHour,
            reminderMinute = saved.reminderMinute,
        )
    }

    LaunchedEffect(
        state.displayMode,
        state.targetMinutes,
        state.sound,
        state.customSoundUri,
        state.reminderEnabled,
        state.reminderHour,
        state.reminderMinute,
    ) {
        if (!state.isRunning) {
            preferences.save(
                TimerPreferences.TimerPrefsSnapshot(
                    displayMode = state.displayMode,
                    targetMinutes = state.targetMinutes,
                    sound = state.sound,
                    customSoundUri = state.customSoundUri,
                    reminderEnabled = state.reminderEnabled,
                    reminderHour = state.reminderHour,
                    reminderMinute = state.reminderMinute,
                ),
            )
            MeditationReminderScheduler.syncFromPreferences(context)
        }
    }

    LaunchedEffect(state.isRunning, state.phase, state.sound, state.customSoundUri) {
        val shouldPlay = state.isRunning && state.phase == TimerPhase.Running
        audioPlayer.sync(state.sound, state.customSoundUri, shouldPlay)
    }

    LaunchedEffect(state.phase) {
        if (state.phase == TimerPhase.Complete) {
            audioPlayer.playCompletionChime()
        }
    }

    LaunchedEffect(state.isRunning) {
        if (state.isRunning) {
            kotlinx.coroutines.delay(2_500)
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && state.reminderEnabled) {
            MeditationReminderScheduler.schedule(context, state.reminderHour, state.reminderMinute)
        }
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.setCustomSoundUri(it.toString()) }
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
        AnimatedVisibility(
            visible = showStatusHeader,
            enter = fadeIn(tween(TextFadeMs)),
            exit = fadeOut(tween(TextFadeMs)),
        ) {
            TimerStatusHeader(state = state)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            TimerDisplay(
                state = state,
                showCountdown = showDisplayCountdown,
                modifier = Modifier.fillMaxSize(),
            )

            FloatingActionButton(
                onClick = { viewModel.toggleRunning() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(FabSize),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = when {
                        state.phase == TimerPhase.Complete -> Icons.Default.PlayArrow
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
                ControlSection(title = "DISPLAY MODE") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TimerDisplayMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.displayMode == mode,
                                onClick = { viewModel.setDisplayMode(mode) },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
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
                    TimerStatCard(
                        label = "DURATION",
                        value = "${state.targetMinutes}",
                        unit = "MIN",
                        onClick = { viewModel.cycleTargetMinutes() },
                        enabled = !state.isRunning,
                        modifier = Modifier.weight(1f),
                    )
                    TimerStatCard(
                        label = "",
                        value = state.remainingFormatted,
                        unit = "",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                ControlSection(title = "AMBIENT SOUND") {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        (TimerSoundOption.entries - TimerSoundOption.Custom).forEach { sound ->
                            FilterChip(
                                selected = state.sound == sound,
                                onClick = { viewModel.setSound(sound) },
                                label = { Text(sound.label, style = MaterialTheme.typography.labelMedium) },
                                enabled = !state.isRunning,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                    selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                ),
                            )
                        }
                        FilterChip(
                            selected = state.sound == TimerSoundOption.Custom,
                            onClick = { soundPickerLauncher.launch("audio/*") },
                            label = { Text("Custom", style = MaterialTheme.typography.labelMedium) },
                            enabled = !state.isRunning,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = if (state.reminderEnabled) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text(
                                    text = "Daily Reminder",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = if (state.reminderEnabled) {
                                        "%02d:%02d".format(state.reminderHour, state.reminderMinute)
                                    } else {
                                        "Off"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = state.reminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!granted) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                    viewModel.setReminder(
                                        enabled = enabled,
                                        hour = state.reminderHour,
                                        minute = state.reminderMinute,
                                    )
                                    if (enabled) {
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                viewModel.setReminder(true, hour, minute)
                                            },
                                            state.reminderHour,
                                            state.reminderMinute,
                                            false,
                                        ).show()
                                    }
                                },
                                enabled = !state.isRunning,
                            )
                        }
                    }
                }

                if (immersive) {
                    Text(
                        text = "Tap anywhere to show or hide controls",
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
private fun TimerDisplay(
    state: TimerSessionState,
    showCountdown: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state.phase == TimerPhase.Prepare) {
        PrepareSequenceOverlay(
            state = state,
            modifier = modifier.padding(bottom = FabClearance),
        )
        return
    }

    when (state.displayMode) {
        TimerDisplayMode.Hourglass -> {
            HourglassCanvas(
                progress = state.progress,
                isRunning = state.isRunning && state.phase == TimerPhase.Running,
                modifier = modifier.padding(bottom = FabClearance),
            )
        }
        TimerDisplayMode.Digital -> {
            Box(
                modifier = modifier.padding(bottom = FabClearance),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = state.remainingFormatted,
                    transitionSpec = {
                        fadeIn(tween(TextFadeMs)) togetherWith fadeOut(tween(TextFadeMs))
                    },
                    label = "digital_countdown",
                ) { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        TimerDisplayMode.Blank -> {
            Box(
                modifier = modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = FabClearance),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedVisibility(
                    visible = showCountdown,
                    enter = fadeIn(tween(TextFadeMs)),
                    exit = fadeOut(tween(TextFadeMs)),
                ) {
                    Text(
                        text = state.remainingFormatted,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepareSequenceOverlay(
    state: TimerSessionState,
    modifier: Modifier = Modifier,
) {
    val inCountdown = state.prepareElapsedMs < TimerPrepareTiming.COUNTDOWN_MS
    val beginElapsed = state.prepareElapsedMs - TimerPrepareTiming.COUNTDOWN_MS
    val beginFadeProgress = when {
        !state.isPrepareBeginVisible -> 0f
        beginElapsed < 400 -> beginElapsed / 400f
        beginElapsed < 400 + TimerPrepareTiming.BEGIN_VISIBLE_MS -> 1f
        else -> {
            val fadeElapsed = beginElapsed - 400 - TimerPrepareTiming.BEGIN_VISIBLE_MS
            (1f - fadeElapsed.toFloat() / TimerPrepareTiming.BEGIN_FADE_MS).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (inCountdown) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Start Meditating In...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AnimatedContent(
                    targetState = state.prepareCountdownSeconds,
                    transitionSpec = {
                        fadeIn(tween(TextFadeMs)) togetherWith fadeOut(tween(TextFadeMs))
                    },
                    label = "prepare_countdown",
                ) { seconds ->
                    Text(
                        text = seconds.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            letterSpacing = 2.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else if (beginFadeProgress > 0f) {
            Text(
                text = "Begin",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 56.sp,
                    letterSpacing = 1.sp,
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = beginFadeProgress),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TimerStatusHeader(state: TimerSessionState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = SereneSpacing.containerMargin)
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.phase == TimerPhase.Idle) {
            Text(
                text = state.statusDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            return
        }

        if (state.statusLabel.isNotEmpty()) {
            AnimatedContent(
                targetState = state.statusLabel,
                transitionSpec = {
                    fadeIn(tween(TextFadeMs)) togetherWith fadeOut(tween(TextFadeMs))
                },
                label = "timer_status",
            ) { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (state.statusDescription.isNotEmpty()) {
            AnimatedContent(
                targetState = state.statusDescription,
                transitionSpec = {
                    fadeIn(tween(TextFadeMs)) togetherWith fadeOut(tween(TextFadeMs))
                },
                label = "timer_description",
                modifier = Modifier.padding(top = 4.dp),
            ) { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp),
        )
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
            content()
        }
    }
}

@Composable
private fun TimerStatCard(
    label: String,
    value: String,
    unit: String,
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
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (unit.isNotEmpty()) {
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
}
