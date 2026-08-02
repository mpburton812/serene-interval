package com.safehaven.affirmations.widget

import android.content.Context
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.mood.MoodSource

class MoodWidgetLogHandler(
    private val record: suspend (MoodSource, Int) -> Long?,
) {
    suspend fun logLevel(level: Int): Long? = record(MoodSource.WIDGET, level)

    companion object {
        fun fromContext(context: Context): MoodWidgetLogHandler =
            MoodWidgetLogHandler { source, level ->
                AppGraph.moodTracker(context.applicationContext).record(source, level)
            }
    }
}
