package com.example.meditationparticles.domain.affirmations

object AffirmationReviewLogic {
    fun canStartReview(affirmationCount: Int): Boolean = affirmationCount > 0

    fun nextIndex(currentIndex: Int, lastIndex: Int): Int? =
        if (currentIndex < lastIndex) currentIndex + 1 else null

    fun previousIndex(currentIndex: Int): Int? =
        if (currentIndex > 0) currentIndex - 1 else null

    fun shouldCompleteReview(currentIndex: Int, lastIndex: Int): Boolean =
        currentIndex == lastIndex
}
