package com.example.meditationparticles.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.isDarkScheme

@Composable
fun SereneTabBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val rotation = remember { AppGraph.tabBackgroundRotation(context) }
    val backgroundDrawable by rotation.currentDrawable.collectAsState()
    val settings = LocalExperienceSettings.current
    val isSystemDark = isSystemInDarkTheme()
    val isDark = isDarkScheme(MaterialTheme.colorScheme)

    LaunchedEffect(settings.themeMode, isSystemDark) {
        rotation.sync(settings.themeMode, isSystemDark)
    }
    val scrim = MaterialTheme.colorScheme.background
    val scrimBrush = if (isDark) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to scrim.copy(alpha = 0.78f),
                0.4f to scrim.copy(alpha = 0.82f),
                1f to scrim.copy(alpha = 0.88f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to scrim.copy(alpha = 0.72f),
                0.4f to scrim.copy(alpha = 0.62f),
                1f to scrim.copy(alpha = 0.52f),
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(backgroundDrawable),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimBrush),
        )
        content()
    }
}
