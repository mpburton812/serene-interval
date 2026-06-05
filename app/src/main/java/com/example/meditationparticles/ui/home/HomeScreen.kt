package com.example.meditationparticles.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.SereneHeaderPlate
import com.example.meditationparticles.ui.components.SereneTabBackground
import com.example.meditationparticles.ui.mood.MoodQuickLogCard
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigate: (SereneDestination, String?) -> Unit,
    onQuickStart: (QuickStartTarget) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val dailyAffirmation by viewModel.dailyAffirmation.collectAsState()
    val quickStartTargets by viewModel.quickStartTargets.collectAsState()
    val settings = LocalExperienceSettings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDailyAffirmation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val quickStartTiles = buildQuickStartTiles(
        selectedTargets = quickStartTargets,
        settings = settings,
        onQuickStart = onQuickStart,
    )

    SereneTabBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
        ) {
        Spacer(modifier = Modifier.height(SereneSpacing.stackSm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
            verticalAlignment = Alignment.Top,
        ) {
            SereneHeaderPlate(
                modifier = Modifier.weight(1f),
                cornerRadius = 20.dp,
            ) {
                Text(
                    text = welcomeHeadline(settings),
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = homeSubtitle(settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 32.dp,
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = "\"$dailyAffirmation\"",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = SereneSpacing.stackMd),
                )
                Row(
                    modifier = Modifier.padding(top = SereneSpacing.stackMd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 1.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                    Text(
                        text = "DAILY AFFIRMATION",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        MoodQuickLogCard(
            onLogMood = viewModel::logMood,
        )

        if (quickStartTiles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
                Text(
                    text = "Quick Start",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
                quickStartTiles.chunked(2).forEach { rowTiles ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
                    ) {
                        rowTiles.forEach { tile ->
                            QuickStartTile(
                                label = tile.label,
                                icon = tile.icon,
                                iconTint = tile.iconTint,
                                iconBackground = tile.iconBackground,
                                onClick = tile.onClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowTiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(SereneSpacing.stackLg))
        }
    }
}

@Composable
private fun QuickStartTile(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        cornerRadius = 40.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = SereneSpacing.stackMd),
            )
        }
    }
}

private fun welcomeHeadline(settings: ExperienceSettings): String {
    val name = settings.preferredName.trim()
    return if (name.isNotEmpty()) {
        "${greeting()}, $name"
    } else {
        greeting()
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good evening"
    }
}

private fun homeSubtitle(settings: ExperienceSettings): String {
    val sanctuary = settings.sanctuaryName.trim()
    return if (sanctuary.isNotEmpty()) {
        "Welcome to $sanctuary."
    } else {
        greetingSubtitle()
    }
}

private fun greetingSubtitle(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "The quiet of the morning is a gift for the soul."
        in 12..16 -> "Take a breath and find your center."
        in 17..20 -> "Let the day settle into stillness."
        else -> "Rest easy — tomorrow begins anew."
    }
}
