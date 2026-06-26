package com.example.meditationparticles.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.settings.BackgroundPeriod
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.SanctuaryLandscapeThemeId
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.settings.backgroundPeriodForTheme
import com.example.meditationparticles.domain.settings.timeOfDayPeriod
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Calendar

@Composable
fun NamingSection(
    sanctuaryName: String,
    onSanctuaryNameChange: (String) -> Unit,
    preferredName: String,
    onPreferredNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        Text(
            text = "Naming",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SanctuaryNameField(
            value = sanctuaryName,
            onValueChange = onSanctuaryNameChange,
        )
        PreferredNameField(
            value = preferredName,
            onValueChange = onPreferredNameChange,
        )
    }
}

@Composable
fun SanctuaryNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "Name your Sway",
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Morning Haven") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
    }
}

@Composable
fun PreferredNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.labelMedium,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Alex") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSection(
    settings: ExperienceSettings,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val currentPeriod = timeOfDayPeriod(hour)

    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeModeChip(
                    label = mode.label,
                    selected = settings.themeMode == mode,
                    onClick = { onThemeModeSelected(mode) },
                )
            }
        }

        if (settings.themeMode == ThemeMode.TimeResponsive) {
            Text(
                text = "Shifts between morning, day, dusk, and night palettes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Right now: ${currentPeriod.label.lowercase()} palette.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ThemeModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandscapeThemeSection(
    settings: ExperienceSettings,
    onLandscapeThemeSelected: (SanctuaryLandscapeThemeId) -> Unit,
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isSystemDark = isSystemInDarkTheme()
    val period = backgroundPeriodForTheme(settings.themeMode, hour, isSystemDark)

    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Tab backdrop",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Choose the landscape behind your tabs. With time-responsive appearance, " +
                "day and night variants shift automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (settings.landscapeThemeId == SanctuaryLandscapeThemeId.Classic) {
            Text(
                text = "You're using the classic tab backgrounds from before landscape themes. " +
                    "Pick a landscape below to switch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SanctuaryLandscapeThemeId.pickerOptions.forEach { theme ->
                LandscapeThemeChip(
                    theme = theme,
                    previewBrush = landscapePreviewBrush(theme, period),
                    selected = settings.landscapeThemeId == theme,
                    onClick = { onLandscapeThemeSelected(theme) },
                )
            }
        }
    }
}

private fun landscapePreviewBrush(
    theme: SanctuaryLandscapeThemeId,
    period: BackgroundPeriod,
): Brush {
    val colors = when (theme) {
        SanctuaryLandscapeThemeId.Beach -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFF8FD3F4), Color(0xFF2E6E8A))
        } else {
            listOf(Color(0xFF1A3A52), Color(0xFF0D1F2D))
        }
        SanctuaryLandscapeThemeId.Cabin -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFF6B8E5A), Color(0xFF3D5C34))
        } else {
            listOf(Color(0xFF2C3E28), Color(0xFF141C12))
        }
        SanctuaryLandscapeThemeId.Desert -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFFE8B86D), Color(0xFFC67B3C))
        } else {
            listOf(Color(0xFF4A3020), Color(0xFF1F140E))
        }
        SanctuaryLandscapeThemeId.Snowscape -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFFE8F4FC), Color(0xFFB8D4E8))
        } else {
            listOf(Color(0xFF2A3A48), Color(0xFF121A22))
        }
        SanctuaryLandscapeThemeId.DeepWoods -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFF3D6B4F), Color(0xFF1E3A2A))
        } else {
            listOf(Color(0xFF142218), Color(0xFF080E0A))
        }
        SanctuaryLandscapeThemeId.Moon -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFF4A5568), Color(0xFF2D3748))
        } else {
            listOf(Color(0xFF1A202C), Color(0xFF0D1017))
        }
        SanctuaryLandscapeThemeId.Space -> if (period == BackgroundPeriod.Daylight) {
            listOf(Color(0xFF1A1A2E), Color(0xFFE8B84A))
        } else {
            listOf(Color(0xFF0A0A14), Color(0xFF1A1A3E))
        }
        SanctuaryLandscapeThemeId.Classic -> listOf(Color(0xFF6B8E9F), Color(0xFF3D5563))
    }
    return Brush.linearGradient(colors)
}

@Composable
fun LandscapeThemeChip(
    theme: SanctuaryLandscapeThemeId,
    previewBrush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewBrush),
        ) {
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
fun ExperienceSection(
    settings: ExperienceSettings,
    onBreathingChanged: (Boolean) -> Unit,
    onTimerChanged: (Boolean) -> Unit,
    onAffirmationsChanged: (Boolean) -> Unit,
    onToolkitChanged: (Boolean) -> Unit,
    onLivingTreeChanged: (Boolean) -> Unit,
    onVisualsChanged: (Boolean) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Your Experience",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Select the tools you want to keep close.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SettingsToggleRow(
            label = "Breathing",
            icon = Icons.Default.Air,
            checked = settings.enableBreathing,
            onCheckedChange = onBreathingChanged,
        )
        SettingsToggleRow(
            label = "Timer",
            icon = Icons.Default.Timer,
            checked = settings.enableTimer,
            onCheckedChange = onTimerChanged,
        )
        SettingsToggleRow(
            label = "Affirmations",
            icon = Icons.Default.AutoAwesome,
            checked = settings.enableAffirmations,
            onCheckedChange = onAffirmationsChanged,
        )
        SettingsToggleRow(
            label = "Toolkit",
            icon = Icons.Default.Handyman,
            checked = settings.enableToolkit,
            onCheckedChange = onToolkitChanged,
        )
        SettingsToggleRow(
            label = "Living Flower",
            icon = Icons.Default.AccountTree,
            checked = settings.enableLivingTree,
            onCheckedChange = onLivingTreeChanged,
        )
        SettingsToggleRow(
            label = "Visual Sanctuary",
            icon = Icons.Default.Landscape,
            checked = settings.enableVisuals,
            onCheckedChange = onVisualsChanged,
        )
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun VisualSanctuarySection(
    enabledScenes: Set<String>,
    onToggleScene: (CalmingVisualizationId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Landscape,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Text(
                text = "Visual Sanctuary Scenes",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(
            text = "Pick your preferred ambient particle scenes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val scenes = listOf(
            SceneOption(CalmingVisualizationId.Snowfall, "Snowfall", Icons.Default.AcUnit),
            SceneOption(CalmingVisualizationId.Rainfall, "Rainfall", Icons.Default.WaterDrop),
            SceneOption(CalmingVisualizationId.Firepit, "Firepit", Icons.Default.LocalFireDepartment),
            SceneOption(CalmingVisualizationId.Sandblow, "Sandblow", Icons.Default.Waves),
            SceneOption(CalmingVisualizationId.Leaffall, "Leaffall", Icons.Default.Eco),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            scenes.chunked(2).forEach { rowScenes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowScenes.forEach { scene ->
                        SceneToggleChip(
                            scene = scene,
                            selected = enabledScenes.contains(scene.id.name),
                            onClick = { onToggleScene(scene.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowScenes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class SceneOption(
    val id: CalmingVisualizationId,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun SceneToggleChip(
    scene: SceneOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = scene.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = scene.label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
