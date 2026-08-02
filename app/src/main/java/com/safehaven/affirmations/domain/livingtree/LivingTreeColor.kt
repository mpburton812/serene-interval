package com.safehaven.affirmations.domain.livingtree

import kotlin.math.abs
import kotlin.math.roundToInt

object LivingTreeColor {
    fun colorArgb(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or
            (red.coerceIn(0, 255) shl 16) or
            (green.coerceIn(0, 255) shl 8) or
            blue.coerceIn(0, 255)

    fun redFromArgb(argb: Int): Int = (argb shr 16) and 0xFF

    fun greenFromArgb(argb: Int): Int = (argb shr 8) and 0xFF

    fun blueFromArgb(argb: Int): Int = argb and 0xFF

    /**
     * Converts HSL to ARGB. Hue is degrees on the color wheel (0–360); saturation and
     * lightness are 0–1. Used for evenly spaced default tag hues.
     */
    fun hslToArgb(hueDegrees: Float, saturation: Float, lightness: Float): Int {
        val hue = ((hueDegrees % 360f) + 360f) % 360f / 360f
        val s = saturation.coerceIn(0f, 1f)
        val l = lightness.coerceIn(0f, 1f)

        if (s == 0f) {
            val gray = (l * 255f).roundToInt()
            return colorArgb(gray, gray, gray)
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        fun hueToChannel(offset: Float): Int {
            val t = (hue + offset) % 1f
            val value = when {
                t < 1f / 6f -> p + (q - p) * 6f * t
                t < 1f / 2f -> q
                t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
                else -> p
            }
            return (value * 255f).roundToInt().coerceIn(0, 255)
        }

        return colorArgb(
            red = hueToChannel(0f),
            green = hueToChannel(1f / 3f),
            blue = hueToChannel(2f / 3f),
        )
    }

    fun distinctDefaultTagColors(count: Int): List<Int> {
        if (count <= 0) return emptyList()
        val step = 360f / count
        return List(count) { index ->
            hslToArgb(
                hueDegrees = index * step,
                saturation = 0.68f,
                lightness = 0.48f,
            )
        }
    }

    fun huesEvenlySpaced(count: Int, toleranceDegrees: Float = 1f): Boolean {
        if (count <= 1) return true
        val expectedStep = 360f / count
        val hues = List(count) { index ->
            val argb = hslToArgb(index * expectedStep, 0.68f, 0.48f)
            hueDegreesFromArgb(argb)
        }.sorted()
        for (index in 1 until hues.size) {
            val delta = hues[index] - hues[index - 1]
            if (abs(delta - expectedStep) > toleranceDegrees) return false
        }
        return true
    }

    private fun hueDegreesFromArgb(argb: Int): Float {
        val r = redFromArgb(argb) / 255f
        val g = greenFromArgb(argb) / 255f
        val b = blueFromArgb(argb) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val hue = when (max) {
            r -> ((g - b) / delta) % 6f
            g -> ((b - r) / delta) + 2f
            else -> ((r - g) / delta) + 4f
        } * 60f
        return if (hue < 0f) hue + 360f else hue
    }
}
