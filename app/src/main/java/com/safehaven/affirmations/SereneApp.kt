package com.safehaven.affirmations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.navigation.PendingToolkitNavigation
import com.safehaven.affirmations.navigation.SereneNavHost
import com.safehaven.affirmations.ui.settings.LocalExperienceSettings
import com.safehaven.affirmations.ui.theme.SereneIntervalTheme
import com.safehaven.affirmations.ui.update.UpdatePromptHost
import com.safehaven.affirmations.ui.update.UpdateViewModel

@Composable
fun SereneApp(
    updateViewModel: UpdateViewModel,
    pendingNavigation: PendingToolkitNavigation? = null,
    pendingFutureSelfMessageId: Long? = null,
) {
    val context = LocalContext.current
    val settingsPreferences = remember { AppGraph.settings(context) }
    val settings by settingsPreferences.settings.collectAsState()

    LaunchedEffect(Unit) {
        if (BuildConfig.UPDATE_CHECK_ENABLED) {
            updateViewModel.checkForUpdate(userInitiated = false)
        }
    }

    LaunchedEffect(Unit) {
        val backupPrefs = AppGraph.autoBackupPreferences(context).load()
        if (backupPrefs.autoBackupEnabled && backupPrefs.isConfigured) {
            AppGraph.autoBackupScheduler(context).apply(backupPrefs)
        }
    }

    SereneIntervalTheme(themeMode = settings.themeMode) {
        CompositionLocalProvider(LocalExperienceSettings provides settings) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                UpdatePromptHost(viewModel = updateViewModel)
                SereneNavHost(
                    updateViewModel = updateViewModel,
                    pendingNavigation = pendingNavigation,
                    pendingFutureSelfMessageId = pendingFutureSelfMessageId,
                )
            }
        }
    }
}
