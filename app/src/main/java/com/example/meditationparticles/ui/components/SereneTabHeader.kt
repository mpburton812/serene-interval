package com.example.meditationparticles.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun SereneTabHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    descriptionTextAlign: TextAlign = TextAlign.Start,
    controls: @Composable RowScope.() -> Unit = {},
    descriptionContent: @Composable (() -> Unit)? = null,
) {
    SereneHeaderPlate(
        modifier = modifier
            .padding(horizontal = SereneSpacing.containerMargin)
            .padding(top = 8.dp, bottom = SereneSpacing.stackSm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = controls,
            )
        }
        when {
            descriptionContent != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    descriptionContent()
                }
            }
            description != null -> {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = descriptionTextAlign,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
