package com.example.meditationparticles.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.meditationparticles.audio.ToolkitAudioRecorder
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun JournalCaptureFields(
    text: String,
    onTextChange: (String) -> Unit,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    pendingAudioPath: String?,
    onPendingAudioChange: (String?) -> Unit,
    onSpeechResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    instructionText: String? = null,
    fieldLabel: String? = null,
    placeholder: String = "What's on your mind…",
    minLines: Int = 8,
    enabled: Boolean = true,
    showMood: Boolean = true,
    showAudioControls: Boolean = true,
    showDictate: Boolean = true,
) {
    val context = LocalContext.current
    val audioRecorder = remember { ToolkitAudioRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) audioRecorder.stop()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            audioRecorder.start()?.let { path ->
                onPendingAudioChange(path)
                isRecording = true
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            spoken?.let(onSpeechResult)
        }
    }

    fun launchSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your thoughts…")
        }
        speechLauncher.launch(intent)
    }

    fun toggleRecording() {
        if (isRecording) {
            val path = audioRecorder.stop()
            isRecording = false
            onPendingAudioChange(path ?: pendingAudioPath)
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                audioRecorder.start()?.let { path ->
                    onPendingAudioChange(path)
                    isRecording = true
                }
            } else {
                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        instructionText?.let { instruction ->
            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        fieldLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showMood) {
            MoodLevelPicker(
                selectedLevel = selectedMoodLevel,
                onLevelChange = onMoodLevelChange,
            )
        }
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            enabled = enabled,
        )
        if (showDictate || showAudioControls) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showDictate) {
                    OutlinedButton(
                        onClick = ::launchSpeechToText,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Dictate", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                if (showAudioControls) {
                    OutlinedButton(
                        onClick = ::toggleRecording,
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    ) {
                        androidx.compose.material3.Icon(
                            if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (isRecording) "Stop" else "Record",
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            if (showAudioControls && pendingAudioPath != null) {
                Text(
                    text = if (isRecording) "Recording in progress…" else "Audio ready to save",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
