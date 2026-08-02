package com.safehaven.affirmations.ui.sanctuary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.quickstart.QuickStartTarget
import com.safehaven.affirmations.domain.settings.ThemeMode
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
import com.safehaven.affirmations.permissions.SchedulingPermissions
import com.safehaven.affirmations.ui.onboarding.OnboardingDraft
import com.safehaven.affirmations.ui.onboarding.toggleQuickStart
import com.safehaven.affirmations.ui.onboarding.toggleToolkitTool
import com.safehaven.affirmations.ui.onboarding.withToolEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SanctuaryWalkthroughViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppGraph.settings(application)
    private val quickStartPreferences = AppGraph.quickStart(application)
    private val toolkitPreferences = AppGraph.toolkit(application)
    private val appContext = application.applicationContext

    private val _draft = MutableStateFlow(loadDraft())
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    private val _walkthroughStep = MutableStateFlow(SanctuaryWalkthroughStep.Welcome)
    val walkthroughStep: StateFlow<SanctuaryWalkthroughStep> = _walkthroughStep.asStateFlow()

    fun reloadFromPreferences() {
        _draft.value = loadDraft()
        _walkthroughStep.value = SanctuaryWalkthroughStep.Welcome
    }

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

    fun setEnableKatiesLoveList(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableKatiesLoveList = enabled) }
    }

    fun setEnableToolkit(enabled: Boolean) {
        _draft.update { draft ->
            if (enabled && draft.enabledToolkitTools.isEmpty()) {
                draft.withToolEnabled(enableToolkit = true)
            } else {
                draft.withToolEnabled(enableToolkit = enabled && draft.enabledToolkitTools.isNotEmpty())
            }
        }
    }

    fun setEnableLivingTree(enabled: Boolean) {
        _draft.update { it.copy(enableLivingTree = enabled) }
    }

    fun toggleToolkitTool(id: ToolkitToolId) {
        _draft.update { it.toggleToolkitTool(id) }
    }

    fun toggleQuickStart(target: QuickStartTarget) {
        _draft.update { it.toggleQuickStart(target) }
    }

    fun setEnableVisuals(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableVisuals = enabled) }
    }

    fun goToStep(step: SanctuaryWalkthroughStep) {
        _walkthroughStep.value = step
    }

    fun goNext(): Boolean {
        val current = _walkthroughStep.value
        val steps = visibleSteps(_draft.value)
        val index = steps.indexOf(current)
        if (index < 0 || index >= steps.lastIndex) return false
        _walkthroughStep.value = steps[index + 1]
        return true
    }

    fun goBack(): Boolean {
        val current = _walkthroughStep.value
        val steps = visibleSteps(_draft.value)
        val index = steps.indexOf(current)
        if (index <= 0) return false
        _walkthroughStep.value = steps[index - 1]
        return true
    }

    fun canAdvanceFrom(step: SanctuaryWalkthroughStep, draft: OnboardingDraft): Boolean = when (step) {
        SanctuaryWalkthroughStep.Welcome -> true
        SanctuaryWalkthroughStep.Name -> true
        SanctuaryWalkthroughStep.Appearance -> true
        SanctuaryWalkthroughStep.Spaces -> draft.enableBreathing || draft.enableTimer ||
            draft.enableAffirmations || draft.enableKatiesLoveList || draft.enableToolkit ||
            draft.enableLivingTree
        SanctuaryWalkthroughStep.QuickStart -> draft.quickStartTargets.size >= 4
        SanctuaryWalkthroughStep.Toolkit -> true
        SanctuaryWalkthroughStep.Review -> draft.canComplete
    }

    fun visibleSteps(draft: OnboardingDraft): List<SanctuaryWalkthroughStep> =
        SanctuaryWalkthroughStep.ordered

    /**
     * Persists sanctuary choices without deleting journal entries or history.
     * Disabled toolkit tools remain in the database and appear on the home timeline.
     */
    fun saveSanctuaryConfiguration(markOnboardingComplete: Boolean) {
        val current = _draft.value
        val exactAlarmsGranted = SchedulingPermissions.canScheduleExactAlarms(appContext)
        val schedulingAvailable = exactAlarmsGranted

        val wasComplete = preferences.load().onboardingCompleted
        val savedSettings = current.toExperienceSettings(
            meditationRemindersAvailable = schedulingAvailable,
            futureSelfSchedulingAvailable = schedulingAvailable,
        ).copy(onboardingCompleted = markOnboardingComplete || wasComplete)

        preferences.save(savedSettings)
        quickStartPreferences.saveSelection(
            current.quickStartTargets,
            savedSettings,
            current.enabledToolkitTools,
        )
        toolkitPreferences.saveEnabledTools(current.enabledToolkitTools)
    }

    private fun loadDraft(): OnboardingDraft {
        val settings = preferences.load()
        val toolkitSnapshot = toolkitPreferences.snapshot.value
        return OnboardingDraft.from(
            settings = settings,
            quickStartTargets = quickStartPreferences.load(settings),
            enabledToolkitTools = toolkitSnapshot.enabledToolIds,
        )
    }
}
