package com.safehaven.affirmations.domain.affirmations

object AffirmationReviewLogic {
    fun canStartReview(affirmationCount: Int): Boolean = affirmationCount > 0

    fun nextIndex(currentIndex: Int, lastIndex: Int): Int? =
        if (currentIndex < lastIndex) currentIndex + 1 else null

    fun previousIndex(currentIndex: Int): Int? =
        if (currentIndex > 0) currentIndex - 1 else null

    fun shouldCompleteReview(currentIndex: Int, lastIndex: Int): Boolean =
        currentIndex == lastIndex

    fun textAt(texts: List<String>, index: Int): String? = texts.getOrNull(index)

    /** Every forward navigation step advances the index by exactly one. */
    fun forwardIndexSequence(lastIndex: Int): List<Int> {
        if (lastIndex < 0) return emptyList()
        return buildList {
            var index = 0
            add(index)
            while (true) {
                val next = nextIndex(index, lastIndex) ?: break
                add(next)
                index = next
            }
        }
    }

    fun canSaveAssessment(moodLevel: Int?, notes: String): Boolean =
        moodLevel != null || notes.trim().isNotEmpty()
}
