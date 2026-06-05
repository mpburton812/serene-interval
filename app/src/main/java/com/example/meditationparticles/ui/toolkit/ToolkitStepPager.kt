package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier

@Composable
fun ToolkitStepPager(
    stepIndex: Int,
    pageCount: Int,
    onStepChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (page: Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = stepIndex) { pageCount }

    LaunchedEffect(stepIndex) {
        if (pagerState.currentPage != stepIndex) {
            pagerState.animateScrollToPage(stepIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != stepIndex) {
            onStepChange(pagerState.settledPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = true,
    ) { page ->
        content(page)
    }
}
