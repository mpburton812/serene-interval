package com.example.meditationparticles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.ui.theme.isDarkScheme

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme(scheme)
    val shape = RoundedCornerShape(cornerRadius)
    val glassFill = if (isDark) {
        scheme.surfaceContainerHigh.copy(alpha = 0.72f)
    } else {
        scheme.surfaceContainerLow.copy(alpha = 0.92f)
    }
    val glassBorder = if (isDark) {
        scheme.outlineVariant.copy(alpha = 0.45f)
    } else {
        scheme.outlineVariant.copy(alpha = 0.65f)
    }
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = scheme.primary.copy(alpha = 0.12f),
                spotColor = scheme.primary.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(glassFill)
            .border(1.dp, glassBorder, shape),
        content = content,
    )
}
