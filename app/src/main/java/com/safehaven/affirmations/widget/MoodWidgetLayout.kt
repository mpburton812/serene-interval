package com.safehaven.affirmations.widget

import com.safehaven.affirmations.domain.mood.MoodScale

object MoodWidgetLayout {
    /** Spacer slots = mood levels + 1 for even edge and inter-button spacing. */
    fun spacerCount(moodLevelCount: Int = MoodScale.MAX - MoodScale.MIN + 1): Int =
        moodLevelCount + 1

    fun moodLevels(): List<Int> = (MoodScale.MIN..MoodScale.MAX).toList()
}
