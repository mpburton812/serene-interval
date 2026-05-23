package com.example.meditationparticles.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val radiusX: Float,
    val radiusY: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val speedPxPerFrame: Float,
    val driftPxPerFrame: Float,
    val color: Color,
)

fun createParticle(
    width: Float,
    height: Float,
    density: Density,
    color: Color,
): Particle {
    val radiusX = with(density) { Random.nextFloat() * 1.4.dp.toPx() + 0.6.dp.toPx() }
    val radiusY = radiusX * (Random.nextFloat() * 0.6f + 0.7f)

    return Particle(
        x = Random.nextFloat() * width,
        y = Random.nextFloat() * (height + with(density) { 200.dp.toPx() }) - with(density) { 100.dp.toPx() },
        radiusX = radiusX,
        radiusY = radiusY,
        rotation = Random.nextFloat() * 360f,
        rotationSpeed = Random.nextFloat() * 2f - 1f,
        speedPxPerFrame = with(density) { Random.nextFloat() * 1.5.dp.toPx() + 0.4.dp.toPx() },
        driftPxPerFrame = with(density) { Random.nextFloat() * 0.6.dp.toPx() - 0.3.dp.toPx() },
        color = color.copy(alpha = Random.nextFloat() * 0.25f + 0.75f),
    )
}

fun createParticles(
    width: Float,
    height: Float,
    density: Density,
    count: Int,
    color: Color,
): List<Particle> {
    if (width <= 0f || height <= 0f || count <= 0) return emptyList()
    return List(count) { createParticle(width, height, density, color) }
}
