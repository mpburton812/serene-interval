package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.meditationparticles.domain.affirmations.AffirmationListKind
import com.example.meditationparticles.ui.components.SereneTabBackground

@Composable
fun AffirmationsScreen(
    modifier: Modifier = Modifier,
    listKind: AffirmationListKind = AffirmationListKind.Affirmations,
) {
    SereneTabBackground(modifier = modifier) {
        AffirmationsTab(
            listKind = listKind,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
