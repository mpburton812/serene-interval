package com.example.meditationparticles.navigation

import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class SereneDestinationMoodGraphTest {
    @Test
    fun moodGraph_route_encodesPeriodName() {
        assertEquals("mood_graph/DAY", SereneDestination.MoodGraph.route(MoodGraphPeriod.DAY))
        assertEquals("mood_graph/WEEK", SereneDestination.MoodGraph.route(MoodGraphPeriod.WEEK))
        assertEquals("mood_graph/MONTH", SereneDestination.MoodGraph.route(MoodGraphPeriod.MONTH))
        assertEquals("mood_graph/CALENDAR", SereneDestination.MoodGraph.route(MoodGraphPeriod.CALENDAR))
    }

    @Test
    fun moodGraph_parsePeriod_defaultsToDayForInvalidArg() {
        assertEquals(MoodGraphPeriod.DAY, SereneDestination.MoodGraph.parsePeriod(null))
        assertEquals(MoodGraphPeriod.DAY, SereneDestination.MoodGraph.parsePeriod("invalid"))
        assertEquals(MoodGraphPeriod.WEEK, SereneDestination.MoodGraph.parsePeriod("WEEK"))
    }
}
