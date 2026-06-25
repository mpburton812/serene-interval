package com.example.meditationparticles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.navigation.clearToolkitNavigationExtras
import com.example.meditationparticles.navigation.toPendingToolkitNavigation
import com.example.meditationparticles.reminder.FutureSelfMessageReceiver
import com.example.meditationparticles.ui.update.UpdateViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val pendingNavigationState = mutableStateOf<PendingToolkitNavigation?>(null)
    private val pendingFutureSelfMessageId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeNavigationIntent(intent)
        deliverOverdueFutureSelfMessages()
        runMoodScaleOneNoteBackfillIfNeeded()
        runMoodTrackerBackfillIfNeeded()
        setContent {
            val updateViewModel: UpdateViewModel = viewModel()
            SereneApp(
                updateViewModel = updateViewModel,
                pendingNavigation = pendingNavigationState.value,
                pendingFutureSelfMessageId = pendingFutureSelfMessageId.value,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeNavigationIntent(intent)
    }

    private fun consumeNavigationIntent(intent: Intent) {
        val navigation = intent.toPendingToolkitNavigation()
        val isFutureSelfNotificationOnly = navigation.futureSelfMessageId != null &&
            navigation.toolkitTab == null &&
            navigation.toolId == null

        if (isFutureSelfNotificationOnly) {
            pendingFutureSelfMessageId.value = navigation.futureSelfMessageId
            pendingNavigationState.value = null
        } else if (navigation.toolkitTab != null || navigation.toolId != null) {
            pendingNavigationState.value = navigation
            pendingFutureSelfMessageId.value = null
        } else {
            pendingNavigationState.value = null
            pendingFutureSelfMessageId.value = null
        }
        intent.clearToolkitNavigationExtras()
    }

    private fun runMoodScaleOneNoteBackfillIfNeeded() {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val oneNotePrefs = AppGraph.oneNotePreferences(applicationContext)
                    if (oneNotePrefs.load().moodScaleBackfillDone) return@withContext
                    val oneNoteSync = AppGraph.oneNoteSync(applicationContext)
                    oneNoteSync.backfillSyncedMoodEntries()
                    oneNotePrefs.setMoodScaleBackfillDone(true)
                }
            }
        }
    }

    private fun runMoodTrackerBackfillIfNeeded() {
        lifecycleScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val moodTrackerPrefs = AppGraph.moodTrackerPreferences(applicationContext)
                    if (moodTrackerPrefs.isBackfillDone()) return@withContext
                    AppGraph.moodTracker(applicationContext).backfillFromLegacyIfNeeded()
                    moodTrackerPrefs.setBackfillDone(true)
                }
            }
        }
    }

    private fun deliverOverdueFutureSelfMessages() {
        lifecycleScope.launch {
            if (!FutureSelfMessageReceiver.canPostNotifications(applicationContext)) return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    val overdue = AppGraph.futureSelfMessages(applicationContext)
                        .getOverdueUndelivered(System.currentTimeMillis())
                    overdue.forEach { message ->
                        FutureSelfMessageReceiver.deliverMessage(applicationContext, message.id)
                    }
                }
            }
        }
    }
}
