package com.example.meditationparticles

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
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.navigation.SereneNavHost
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.SereneIntervalTheme
import com.example.meditationparticles.ui.update.UpdatePromptHost
import com.example.meditationparticles.ui.update.UpdateViewModel

@Composable
fun SereneApp(
    updateViewModel: UpdateViewModel,
    pendingNavigation: PendingToolkitNavigation? = null,
) {
    val context = LocalContext.current
    val settingsPreferences = remember { AppGraph.settings(context) }
    val settings by settingsPreferences.settings.collectAsState()

    LaunchedEffect(Unit) {
        if (BuildConfig.UPDATE_CHECK_ENABLED) {
            updateViewModel.checkForUpdate(userInitiated = false)
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
                )
            }
        }
    }
}
