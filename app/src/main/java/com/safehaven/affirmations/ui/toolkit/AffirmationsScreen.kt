package com.safehaven.affirmations.ui.toolkit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind
import com.safehaven.affirmations.ui.components.SereneTabBackground

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
