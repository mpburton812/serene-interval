package com.safehaven.affirmations.widget

import com.safehaven.affirmations.domain.mood.MoodSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MoodWidgetLogHandlerTest {
    @Test
    fun logLevel_recordsWithWidgetSource() = runTest {
        var capturedSource: MoodSource? = null
        var capturedLevel: Int? = null
        val handler = MoodWidgetLogHandler { source, level ->
            capturedSource = source
            capturedLevel = level
            99L
        }

        val result = handler.logLevel(level = 3)

        assertEquals(MoodSource.WIDGET, capturedSource)
        assertEquals(3, capturedLevel)
        assertEquals(99L, result)
    }
}
