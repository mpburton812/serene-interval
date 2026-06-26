package com.example.meditationparticles.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.meditationparticles.domain.settings.ThemeMode
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun SereneIntervalTheme(
    themeMode: ThemeMode = ThemeMode.TimeResponsive,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    var currentHour by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    LaunchedEffect(themeMode) {
        if (themeMode == ThemeMode.TimeResponsive) {
            while (true) {
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                delay(60_000L)
            }
        }
    }

    val colorScheme = remember(themeMode, isSystemDark, currentHour) {
        resolveColorScheme(themeMode, isSystemDark, currentHour)
    }
    val typography = remember(colorScheme) { sereneTypography(colorScheme) }
    val useDarkStatusBarIcons = !isDarkScheme(colorScheme)
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                useDarkStatusBarIcons
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}

/** @deprecated Use SereneIntervalTheme */
@Composable
fun MeditationParticlesTheme(content: @Composable () -> Unit) {
    SereneIntervalTheme(content = content)
}
