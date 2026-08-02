package com.safehaven.affirmations.ui.toolkit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.data.local.AffirmationEntity
import com.safehaven.affirmations.domain.affirmations.AffirmationReviewBeadColors
import com.safehaven.affirmations.ui.theme.SereneSpacing

private const val ReviewTransitionMs = 200

@Composable
fun AffirmationReviewOverlay(
    affirmations: List<AffirmationEntity>,
    currentIndex: Int,
    currentAffirmation: AffirmationEntity?,
    isCompleting: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastIndex = affirmations.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = SereneSpacing.containerMargin, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReviewBeadRow(
                total = affirmations.size,
                currentIndex = currentIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = SereneSpacing.stackLg),
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(currentIndex, isCompleting, lastIndex) {
                        if (isCompleting) return@pointerInput
                        detectTapGestures { offset ->
                            if (offset.x < size.width / 2f) {
                                if (currentIndex > 0) onPrevious()
                            } else {
                                onNext()
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        fadeIn(tween(ReviewTransitionMs)) togetherWith fadeOut(tween(ReviewTransitionMs))
                    },
                    label = "review_affirmation_text",
                ) { index ->
                    Text(
                        text = affirmations.getOrNull(index)?.text.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SereneSpacing.stackMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onPrevious,
                    enabled = !isCompleting && currentIndex > 0,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("Previous", modifier = Modifier.padding(start = 4.dp))
                }

                TextButton(
                    onClick = onExit,
                    enabled = !isCompleting,
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Exit", modifier = Modifier.padding(start = 4.dp))
                }

                TextButton(
                    onClick = onNext,
                    enabled = !isCompleting && currentAffirmation != null,
                ) {
                    Text(if (currentIndex == lastIndex) "Finish" else "Next")
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewBeadRow(
    total: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val isCurrent = index == currentIndex
            val beadColor = AffirmationReviewBeadColors.colorForIndex(index, total)
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) {
                            beadColor
                        } else {
                            beadColor.copy(alpha = 0.55f)
                        },
                    ),
            )
        }
    }
}
