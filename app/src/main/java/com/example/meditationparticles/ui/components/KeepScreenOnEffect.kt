package com.example.meditationparticles.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

@Composable
fun KeepScreenOnEffect(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        if (active) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }
}
