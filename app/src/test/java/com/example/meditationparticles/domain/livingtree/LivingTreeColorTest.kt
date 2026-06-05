package com.example.meditationparticles.domain.livingtree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeColorTest {
    @Test
    fun colorArgb_roundTripsRgbChannels() {
        val argb = LivingTreeColor.colorArgb(red = 12, green = 34, blue = 56)

        assertEquals(12, LivingTreeColor.redFromArgb(argb))
        assertEquals(34, LivingTreeColor.greenFromArgb(argb))
        assertEquals(56, LivingTreeColor.blueFromArgb(argb))
    }

    @Test
    fun colorArgb_clampsChannelValues() {
        val argb = LivingTreeColor.colorArgb(red = -5, green = 300, blue = 128)

        assertEquals(0, LivingTreeColor.redFromArgb(argb))
        assertEquals(255, LivingTreeColor.greenFromArgb(argb))
        assertEquals(128, LivingTreeColor.blueFromArgb(argb))
    }

    @Test
    fun distinctDefaultTagColors_evenlySpacesTwelveHues() {
        val colors = LivingTreeColor.distinctDefaultTagColors(12)

        assertEquals(12, colors.size)
        assertEquals(colors.toSet().size, colors.size)
        assertTrue(LivingTreeColor.huesEvenlySpaced(12))
    }

    @Test
    fun defaultTags_useDistinctPresetColors() {
        assertEquals(12, LivingTreeDefaults.defaultTags.size)
        assertEquals(LivingTreeDefaults.defaultTags.size, LivingTreeDefaults.presetColors.size)
        assertEquals(
            LivingTreeDefaults.defaultTags.map { it.colorArgb },
            LivingTreeDefaults.presetColors,
        )
    }
}
