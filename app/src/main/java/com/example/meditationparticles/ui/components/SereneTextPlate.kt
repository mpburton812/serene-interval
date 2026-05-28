package com.example.meditationparticles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun SereneTextPlate(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    showBorder: Boolean = true,
    contentPadding: Dp = SereneSpacing.stackSm,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = sereneGlassStyle(MaterialTheme.colorScheme)
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(glass.fill)
            .then(
                if (showBorder) {
                    Modifier.border(1.dp, glass.border, shape)
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SereneHeaderPlate(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    SereneTextPlate(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        contentPadding = SereneSpacing.stackSm,
    ) {
        Column(content = content)
    }
}
