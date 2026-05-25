package com.example.meditationparticles.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.sessions.HomeProgress
import com.example.meditationparticles.domain.sessions.MeditationSession
import com.example.meditationparticles.domain.sessions.SessionType
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.theme.isDarkScheme
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigate: (SereneDestination, String?) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val dailyAffirmation by viewModel.dailyAffirmation.collectAsState()
    val homeProgress by viewModel.homeProgress.collectAsState()
    val settings = LocalExperienceSettings.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isDarkScheme(MaterialTheme.colorScheme)
    val scrimTopAlpha = if (isDark) 0.78f else 0.58f
    val scrimBottomAlpha = if (isDark) 0.88f else 0.72f

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

    val quickStartTiles = buildList {
        if (settings.enableBreathing) {
            add(
                QuickStartItem(
                    label = "Breathing",
                    icon = Icons.Default.Air,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    iconBackground = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    onClick = { onNavigate(SereneDestination.Breathe, null) },
                ),
            )
        }
        if (settings.enableTimer) {
            add(
                QuickStartItem(
                    label = "Meditation",
                    icon = Icons.Default.SelfImprovement,
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    onClick = { onNavigate(SereneDestination.Timer, null) },
                ),
            )
        }
        if (settings.enableAffirmations) {
            add(
                QuickStartItem(
                    label = "Affirmations",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                    onClick = {
                        onNavigate(SereneDestination.Affirmations, null)
                    },
                ),
            )
        }
        if (settings.enableToolkit) {
            add(
                QuickStartItem(
                    label = "Toolkit",
                    icon = Icons.Default.Handyman,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = {
                        onNavigate(SereneDestination.Toolkit, null)
                    },
                ),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.home_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = scrimTopAlpha),
                            MaterialTheme.colorScheme.background.copy(alpha = scrimBottomAlpha),
                        ),
                    ),
                ),
        )
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${greeting()}, ${settings.displayName}",
                    style = MaterialTheme.typography.displayLarge,
                )
                Text(
                    text = homeSubtitle(settings),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        DailyProgressCard(progress = homeProgress)

        if (homeProgress.recentSessions.isNotEmpty()) {
            RecentActivitiesSection(
                sessions = homeProgress.recentSessions,
                onSessionClick = { session ->
                    navigateToSession(session, onNavigate)
                },
            )
        }

        Spacer(modifier = Modifier.height(SereneSpacing.stackLg))
        }
    }
}

@Composable
private fun DailyProgressCard(progress: HomeProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Daily Progress",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text = progress.streakDays.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "Day Streak",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Calm Meter",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${progress.calmMeterPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.calmMeterPercent / 100f)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        text = progress.calmMeterMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (progress.todayMinutes > 0) {
                        Text(
                            text = "${progress.todayMinutes} min of calm today",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentActivitiesSection(
    sessions: List<MeditationSession>,
    onSessionClick: (MeditationSession) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Recent Activities",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sessions.forEach { session ->
                RecentActivityRow(
                    session = session,
                    onClick = { onSessionClick(session) },
                )
            }
        }
    }
}

@Composable
private fun RecentActivityRow(
    session: MeditationSession,
    onClick: () -> Unit,
) {
    val (icon, iconTint, iconBackground) = sessionIconStyle(session.type)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                    )
                }
                Column {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = "${formatSessionDuration(session.durationSeconds)} • " +
                            formatRelativeSessionTime(session.completedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun sessionIconStyle(type: SessionType): Triple<ImageVector, Color, Color> {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        SessionType.TIMER -> Triple(
            Icons.Default.Timer,
            scheme.secondary,
            scheme.secondaryContainer.copy(alpha = 0.4f),
        )
        SessionType.BREATHING -> Triple(
            Icons.Default.Spa,
            scheme.primary,
            scheme.primaryContainer.copy(alpha = 0.35f),
        )
        SessionType.VISUALIZATION -> Triple(
            Icons.Default.Landscape,
            scheme.tertiary,
            scheme.tertiaryContainer.copy(alpha = 0.25f),
        )
    }
}

private fun navigateToSession(
    session: MeditationSession,
    onNavigate: (SereneDestination, String?) -> Unit,
) {
    when (session.type) {
        SessionType.TIMER -> onNavigate(SereneDestination.Timer, null)
        SessionType.BREATHING -> onNavigate(SereneDestination.Breathe, null)
        SessionType.VISUALIZATION -> onNavigate(SereneDestination.Visualizations, null)
    }
}

private data class QuickStartItem(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val onClick: () -> Unit,
)

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
