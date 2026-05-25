package com.example.meditationparticles.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import com.example.meditationparticles.permissions.SchedulingPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppGraph.settings(application)
    private val appContext = application.applicationContext

    private val _draft = MutableStateFlow(OnboardingDraft.from(preferences.load()))
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    fun setPreferredName(name: String) {
        _draft.update { it.copy(preferredName = name) }
    }

    fun setSanctuaryName(name: String) {
        _draft.update { it.copy(sanctuaryName = name) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _draft.update { it.copy(themeMode = mode) }
    }

    fun setEnableBreathing(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableBreathing = enabled) }
    }

    fun setEnableTimer(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableTimer = enabled) }
    }

    fun setEnableAffirmations(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableAffirmations = enabled) }
    }

    fun setEnableToolkit(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableToolkit = enabled) }
    }

    fun toggleToolkitTool(id: ToolkitToolId) {
        _draft.update { it.toggleToolkitTool(id) }
    }

    fun setEnableVisuals(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableVisuals = enabled) }
    }

    fun toggleScene(id: CalmingVisualizationId) {
        _draft.update { it.toggleScene(id) }
    }

    fun continueFromCustomization(): Boolean {
        val current = _draft.value
        if (!current.canComplete) return false
        if (needsExactAlarmStep()) {
            _draft.update {
                it.copy(
                    step = OnboardingStep.ExactAlarms,
                    permissionState = it.permissionState.copy(
                        exactAlarmsGranted = SchedulingPermissions.canScheduleExactAlarms(appContext),
                        exactAlarmsChecked = SchedulingPermissions.canScheduleExactAlarms(appContext),
                    ),
                )
            }
            return false
        }
        if (needsNotificationStep(exactAlarmsGranted = true)) {
            _draft.update {
                it.copy(
                    step = OnboardingStep.Notifications,
                    permissionState = it.permissionState.copy(
                        exactAlarmsGranted = true,
                        exactAlarmsChecked = true,
                        notificationsGranted = SchedulingPermissions.canPostNotifications(appContext),
                        notificationsChecked = SchedulingPermissions.canPostNotifications(appContext),
                    ),
                )
            }
            return false
        }
        completeOnboarding()
        return true
    }

    fun openExactAlarmSettings() {
        SchedulingPermissions.openExactAlarmSettings(appContext)
        _draft.update {
            it.copy(permissionState = it.permissionState.copy(awaitingSettingsReturn = true))
        }
    }

    fun openNotificationSettings() {
        SchedulingPermissions.openNotificationSettings(appContext)
        _draft.update {
            it.copy(permissionState = it.permissionState.copy(awaitingSettingsReturn = true))
        }
    }

    fun markAwaitingNotificationPermissionRequest() {
        _draft.update {
            it.copy(permissionState = it.permissionState.copy(awaitingSettingsReturn = false))
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _draft.update {
            it.copy(
                permissionState = it.permissionState.copy(
                    notificationsGranted = granted,
                    notificationsChecked = true,
                    awaitingSettingsReturn = false,
                ),
            )
        }
    }

    fun refreshPermissionsOnResume() {
        val exactAlarmsGranted = SchedulingPermissions.canScheduleExactAlarms(appContext)
        val notificationsGranted = SchedulingPermissions.canPostNotifications(appContext)
        _draft.update { draft ->
            val permission = draft.permissionState
            val wasAwaiting = permission.awaitingSettingsReturn
            draft.copy(
                permissionState = permission.copy(
                    exactAlarmsGranted = exactAlarmsGranted,
                    exactAlarmsChecked = permission.exactAlarmsChecked || wasAwaiting || exactAlarmsGranted,
                    notificationsGranted = notificationsGranted,
                    notificationsChecked = permission.notificationsChecked || wasAwaiting || notificationsGranted,
                    awaitingSettingsReturn = false,
                ),
            )
        }
    }

    /**
     * Refreshes permission state on resume. When the user returns from exact-alarm settings
     * with permission granted, advances to the next onboarding step automatically.
     *
     * @return true if onboarding completed and the caller should navigate away
     */
    fun onResume(): Boolean {
        val current = _draft.value
        val wasAwaitingSettingsReturn = current.permissionState.awaitingSettingsReturn

        if (current.step == OnboardingStep.ExactAlarms && wasAwaitingSettingsReturn) {
            refreshPermissionsOnResume()
            if (SchedulingPermissions.canScheduleExactAlarms(appContext)) {
                return continueFromExactAlarms()
            }
            return false
        }

        refreshPermissionsOnResume()
        return false
    }

    fun continueFromExactAlarms(): Boolean {
        refreshPermissionsOnResume()
        val exactAlarmsGranted = SchedulingPermissions.canScheduleExactAlarms(appContext)
        if (exactAlarmsGranted && needsNotificationStep(exactAlarmsGranted = true)) {
            _draft.update {
                it.copy(
                    step = OnboardingStep.Notifications,
                    permissionState = it.permissionState.copy(
                        exactAlarmsGranted = true,
                        exactAlarmsChecked = true,
                        notificationsGranted = SchedulingPermissions.canPostNotifications(appContext),
                        notificationsChecked = SchedulingPermissions.canPostNotifications(appContext),
                    ),
                )
            }
            return false
        }
        completeOnboarding()
        return true
    }

    fun continueFromNotifications(): Boolean {
        refreshPermissionsOnResume()
        completeOnboarding()
        return true
    }

    fun completeOnboarding() {
        val current = _draft.value
        if (current.step == OnboardingStep.Customization && !current.canComplete) return

        val exactAlarmsGranted = SchedulingPermissions.canScheduleExactAlarms(appContext)
        val schedulingAvailable = exactAlarmsGranted

        preferences.save(
            current.toExperienceSettings(
                meditationRemindersAvailable = schedulingAvailable,
                futureSelfSchedulingAvailable = schedulingAvailable,
            ),
        )
        if (current.enableToolkit) {
            AppGraph.toolkit(getApplication()).saveConfiguration(current.enabledToolkitTools)
        }
    }

    private fun needsExactAlarmStep(): Boolean {
        return SchedulingPermissions.needsExactAlarmPermission() &&
            !SchedulingPermissions.canScheduleExactAlarms(appContext)
    }

    private fun needsNotificationStep(exactAlarmsGranted: Boolean): Boolean {
        return exactAlarmsGranted &&
            !SchedulingPermissions.canPostNotifications(appContext)
    }
}
