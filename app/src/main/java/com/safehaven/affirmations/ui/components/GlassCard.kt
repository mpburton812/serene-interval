package com.safehaven.affirmations.ui.components

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
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurfaceCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        glassStyle = sereneGlassStyle(MaterialTheme.colorScheme),
        content = content,
    )
}

@Composable
fun HistoryGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurfaceCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        glassStyle = sereneHistoryGlassStyle(MaterialTheme.colorScheme),
        content = content,
    )
}

@Composable
private fun GlassSurfaceCard(
    modifier: Modifier,
    cornerRadius: Dp,
    glassStyle: SereneGlassStyle,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = shape,
                ambientColor = scheme.primary.copy(alpha = 0.12f),
                spotColor = scheme.primary.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(glassStyle.fill)
            .border(1.dp, glassStyle.border, shape),
        content = content,
    )
}
