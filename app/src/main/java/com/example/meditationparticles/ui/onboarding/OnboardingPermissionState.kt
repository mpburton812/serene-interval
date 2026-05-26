package com.example.meditationparticles.ui.onboarding

enum class OnboardingStep {
    Customization,
    ExactAlarms,
    Notifications,
    OneNoteConnect,
}

data class OnboardingPermissionState(
    val exactAlarmsGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val exactAlarmsChecked: Boolean = false,
    val notificationsChecked: Boolean = false,
    val awaitingSettingsReturn: Boolean = false,
)
