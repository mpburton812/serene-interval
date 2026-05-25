package com.example.meditationparticles

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.navigation.clearToolkitNavigationExtras
import com.example.meditationparticles.navigation.toPendingToolkitNavigation
import com.example.meditationparticles.ui.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    private val pendingNavigationState = mutableStateOf<PendingToolkitNavigation?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        consumeToolkitNavigation(intent)
        setContent {
            val updateViewModel: UpdateViewModel = viewModel()
            SereneApp(
                updateViewModel = updateViewModel,
                pendingNavigation = pendingNavigationState.value,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeToolkitNavigation(intent)
    }

    private fun consumeToolkitNavigation(intent: Intent) {
        pendingNavigationState.value = intent.toPendingToolkitNavigation()
        intent.clearToolkitNavigationExtras()
    }
}
