package com.example.meditationparticles.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.BuildConfig
import com.example.meditationparticles.R
import com.example.meditationparticles.permissions.SchedulingPermissions
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.sanctuary.SanctuaryWalkthroughMode
import com.example.meditationparticles.ui.sanctuary.SanctuaryWalkthroughScreen
import com.example.meditationparticles.ui.sanctuary.SanctuaryWalkthroughViewModel
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val draft by viewModel.draft.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val onCompleteUpdated by rememberUpdatedState(onComplete)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && viewModel.onResume()) {
                onCompleteUpdated()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        if (draft.step != OnboardingStep.Customization) {
            Spacer(modifier = Modifier.height(SereneSpacing.stackMd))
            OnboardingHeader(step = draft.step)
        }

        when (draft.step) {
            OnboardingStep.Customization -> {
                val walkthroughViewModel: SanctuaryWalkthroughViewModel = viewModel()
                SanctuaryWalkthroughScreen(
                    mode = SanctuaryWalkthroughMode.FirstVisit,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = SereneSpacing.containerMargin),
                    viewModel = walkthroughViewModel,
                    onFinished = {
                        viewModel.applyWalkthroughDraft(walkthroughViewModel.draft.value)
                        if (viewModel.continueFromWalkthrough()) {
                            onComplete()
                        }
                    },
                    onBackOut = {},
                )
            }
            OnboardingStep.ExactAlarms -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SereneSpacing.containerMargin),
                ) {
                    OnboardingExactAlarmsStep(
                        permissionState = draft.permissionState,
                        onOpenSettings = viewModel::openExactAlarmSettings,
                        onContinue = {
                            if (viewModel.continueFromExactAlarms()) onComplete()
                        },
                    )
                }
            }
            OnboardingStep.Notifications -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SereneSpacing.containerMargin),
                ) {
                    OnboardingNotificationsStep(
                        permissionState = draft.permissionState,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                viewModel.markAwaitingNotificationPermissionRequest()
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onOpenSettings = viewModel::openNotificationSettings,
                        onContinue = {
                            if (viewModel.continueFromNotifications()) onComplete()
                        },
                    )
                }
            }
            OnboardingStep.OneNoteConnect -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = SereneSpacing.containerMargin),
                ) {
                    val activity = LocalActivity.current as? ComponentActivity
                    OnboardingOneNoteConnectStep(
                        onConnect = {
                            activity?.let { host ->
                                viewModel.connectOneNote(host) { connected ->
                                    if (connected && viewModel.continueFromOneNoteConnect()) {
                                        onComplete()
                                    }
                                }
                            }
                        },
                        onSkip = {
                            viewModel.skipOneNoteConnect()
                            if (viewModel.continueFromOneNoteConnect()) onComplete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: OnboardingStep) {
    val appName = stringResource(R.string.app_name)
    val (title, subtitle) = when (step) {
        OnboardingStep.Customization -> "Let's Build Your Sanctuary" to
            "Let's shape a space that feels uniquely yours."
        OnboardingStep.ExactAlarms -> "Alarms & Reminders" to
            "Scheduled features need permission to deliver on time."
        OnboardingStep.Notifications -> "Notifications" to
            "Allow $appName to notify you when reminders are due."
        OnboardingStep.OneNoteConnect -> "Connect OneNote" to
            "Optionally sync saved journal entries to Microsoft OneNote."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun OnboardingExactAlarmsStep(
    permissionState: OnboardingPermissionState,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val granted = permissionState.exactAlarmsGranted
    val checked = permissionState.exactAlarmsChecked

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "$appName uses Alarms & reminders to schedule meditation reminders " +
                    "and Future Self messages at the exact time you choose.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Tap Open settings, enable Alarms & reminders for this app, then return here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open settings")
            }
            if (checked) {
                Text(
                    text = if (granted) {
                        "Alarms & reminders are enabled."
                    } else {
                        "Alarms & reminders are still off. Meditation reminder and Future Self Message " +
                            "will be disabled until you enable this in system settings."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                )
            }
        }
    }

    OnboardingPrimaryButton(
        text = if (checked && !granted) "Continue without scheduling" else "Continue",
        enabled = true,
        onClick = onContinue,
    )
}

@Composable
private fun OnboardingNotificationsStep(
    permissionState: OnboardingPermissionState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onContinue: () -> Unit,
) {
    val appName = stringResource(R.string.app_name)
    val granted = permissionState.notificationsGranted
    val checked = permissionState.notificationsChecked
    val needsRuntimeRequest = SchedulingPermissions.needsRuntimeNotificationPermission()

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Notifications let $appName alert you for daily meditation reminders " +
                    "and scheduled Future Self messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (needsRuntimeRequest) {
                OutlinedButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Allow notifications")
                }
            }
            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open notification settings")
            }
            if (checked) {
                Text(
                    text = if (granted) {
                        "Notifications are enabled."
                    } else {
                        "Notifications are still off. You can enable them later in system settings " +
                            "when you turn on a reminder or schedule a message."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }

    OnboardingPrimaryButton(
        text = "Continue",
        enabled = true,
        onClick = onContinue,
    )
}

@Composable
private fun OnboardingOneNoteConnectStep(
    onConnect: () -> Unit,
    onSkip: () -> Unit,
) {
    if (!BuildConfig.ONENOTE_SYNC_AVAILABLE) {
        OnboardingPrimaryButton(
            text = "Enter Your Sanctuary",
            enabled = true,
            onClick = onSkip,
        )
        return
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "When connected, new toolkit journal entries sync to a OneNote section " +
                    "named \"Serene Interval\". Audio stays in the app only.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect Microsoft account")
            }
        }
    }

    OnboardingPrimaryButton(
        text = "Enter Your Sanctuary",
        enabled = true,
        onClick = onSkip,
    )

    OutlinedButton(
        onClick = onSkip,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Skip for now")
    }
}

@Composable
private fun OnboardingSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
            content = { content() },
        )
    }
}

@Composable
private fun OnboardingPrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SereneSpacing.stackLg),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}
