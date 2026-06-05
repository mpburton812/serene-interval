package com.example.meditationparticles.domain.livingtree

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingTreeTextContrastTest {
    @Test
    fun relativeLuminance_whiteIsBrighterThanBlack() {
        val white = LivingTreeTextContrast.relativeLuminance(Color.White)
        val black = LivingTreeTextContrast.relativeLuminance(Color.Black)

        assertTrue(white > black)
    }

    @Test
    fun textOnBubbleFill_darkColors_prefersLightText() {
        val text = LivingTreeTextContrast.textOnBubbleFill(
            bubbleColors = listOf(Color(0xFF112233), Color(0xFF223344)),
            scheme = lightColorScheme(),
            isDark = false,
        )

        assertTrue(text.red + text.green + text.blue > 2.0f)
    }

    @Test
    fun textOnBubbleFill_lightColors_prefersDarkText() {
        val text = LivingTreeTextContrast.textOnBubbleFill(
            bubbleColors = listOf(Color(0xFFF7A8B8), Color(0xFF55CDFC)),
            scheme = lightColorScheme(),
            isDark = false,
        )

        assertTrue(text.red + text.green + text.blue < 1.5f)
    }

    @Test
    fun labelColors_provideDistinctPlateAndText() {
        val dark = LivingTreeTextContrast.labelColors(darkColorScheme(), isDark = true)
        val light = LivingTreeTextContrast.labelColors(lightColorScheme(), isDark = false)

        assertTrue(dark.plateFill.alpha > 0.5f)
        assertTrue(light.plateFill.alpha > 0.5f)
        assertTrue(dark.text.alpha > 0.8f)
        assertTrue(light.text.alpha > 0.8f)
    }
}
