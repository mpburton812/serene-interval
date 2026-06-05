package com.example.meditationparticles.ui.mood

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.moodColor
import com.example.meditationparticles.ui.components.moodIcon
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun MoodQuickLogCard(
    onLogMood: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pulsingLevel by remember { mutableIntStateOf(0) }
    val scales = remember {
        (MoodScale.MIN..MoodScale.MAX).associateWith { Animatable(1f) }
    }

    LaunchedEffect(pulsingLevel) {
        if (pulsingLevel == 0) return@LaunchedEffect
        val animatable = scales[pulsingLevel] ?: return@LaunchedEffect
        repeat(3) {
            animatable.animateTo(1.2f, animationSpec = tween(120))
            animatable.animateTo(1f, animationSpec = tween(120))
        }
        onLogMood(pulsingLevel)
        pulsingLevel = 0
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Text(
                text = "How are you feeling?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                (MoodScale.MIN..MoodScale.MAX).forEach { level ->
                    val scale = scales.getValue(level)
                    MoodQuickLogButton(
                        level = level,
                        scale = scale.value,
                        enabled = pulsingLevel == 0,
                        onClick = { pulsingLevel = level },
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodQuickLogButton(
    level: Int,
    scale: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = moodColor(level)
    Box(
        modifier = modifier
            .scale(scale)
            .size(64.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = moodIcon(level),
            contentDescription = "Log mood ${MoodScale.label(level)}",
            tint = color,
            modifier = Modifier.size(48.dp),
        )
    }
}
