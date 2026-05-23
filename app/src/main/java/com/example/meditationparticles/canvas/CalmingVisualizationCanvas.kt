package com.example.meditationparticles.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import kotlinx.coroutines.isActive
import kotlin.math.sin
import kotlin.random.Random

private data class VizParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var alpha: Float,
    val color: Color,
)

class VisualizationParticleSystem(
    private val visualizationId: CalmingVisualizationId,
    private val particleCount: Int,
) {
    private var particles = emptyList<VizParticle>()
    private var width = 0f
    private var height = 0f

    fun resize(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        if (w == width && h == height && particles.isNotEmpty()) return
        width = w
        height = h
        particles = List(particleCount) { spawn(Random.nextFloat() * w, Random.nextFloat() * h) }
    }

    fun update(isPlaying: Boolean) {
        if (!isPlaying || width <= 0f || height <= 0f) return
        particles.forEach { particle ->
            when (visualizationId) {
                CalmingVisualizationId.Firepit -> updateFire(particle)
                CalmingVisualizationId.Sandblow -> updateSand(particle)
                CalmingVisualizationId.Rainfall -> updateRain(particle)
                CalmingVisualizationId.Leaffall -> updateLeaf(particle)
                CalmingVisualizationId.Snowfall -> updateSnow(particle)
            }
        }
    }

    fun draw(scope: DrawScope) {
        particles.forEach { particle ->
            when (visualizationId) {
                CalmingVisualizationId.Rainfall -> scope.drawRainDrop(particle)
                CalmingVisualizationId.Leaffall -> scope.drawLeaf(particle)
                CalmingVisualizationId.Firepit -> scope.drawEmber(particle)
                else -> scope.drawSoftDot(particle)
            }
        }
    }

    private fun spawn(x: Float, y: Float): VizParticle = when (visualizationId) {
        CalmingVisualizationId.Snowfall -> VizParticle(
            x = x, y = y,
            size = Random.nextFloat() * 3f + 1.5f,
            speedY = Random.nextFloat() * 0.8f + 0.3f,
            speedX = Random.nextFloat() * 0.4f - 0.2f,
            rotation = 0f, rotationSpeed = 0f,
            alpha = Random.nextFloat() * 0.4f + 0.5f,
            color = Color.White,
        )
        CalmingVisualizationId.Rainfall -> VizParticle(
            x = x, y = y,
            size = Random.nextFloat() * 12f + 8f,
            speedY = Random.nextFloat() * 6f + 8f,
            speedX = -1f,
            rotation = 0f, rotationSpeed = 0f,
            alpha = Random.nextFloat() * 0.25f + 0.35f,
            color = Color(0xFF9EC4E8),
        )
        CalmingVisualizationId.Firepit -> VizParticle(
            x = x, y = y,
            size = Random.nextFloat() * 4f + 2f,
            speedY = -(Random.nextFloat() * 1.5f + 0.8f),
            speedX = Random.nextFloat() * 0.6f - 0.3f,
            rotation = 0f, rotationSpeed = 0f,
            alpha = Random.nextFloat() * 0.5f + 0.4f,
            color = if (Random.nextBoolean()) Color(0xFFFF8C42) else Color(0xFFE86B6B),
        )
        CalmingVisualizationId.Sandblow -> VizParticle(
            x = x, y = y,
            size = Random.nextFloat() * 2.5f + 1f,
            speedY = Random.nextFloat() * 0.3f - 0.15f,
            speedX = Random.nextFloat() * 2f + 1f,
            rotation = 0f, rotationSpeed = 0f,
            alpha = Random.nextFloat() * 0.3f + 0.35f,
            color = Color(0xFFD4A574),
        )
        CalmingVisualizationId.Leaffall -> VizParticle(
            x = x, y = y,
            size = Random.nextFloat() * 6f + 4f,
            speedY = Random.nextFloat() * 1.2f + 0.6f,
            speedX = Random.nextFloat() * 0.8f - 0.4f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 2f - 1f,
            alpha = Random.nextFloat() * 0.3f + 0.65f,
            color = listOf(
                Color(0xFFC87840),
                Color(0xFFB85030),
                Color(0xFFD4A040),
                Color(0xFF8D4D3B),
            ).random(),
        )
    }

    private fun updateSnow(p: VizParticle) {
        p.y += p.speedY
        p.x += p.speedX + sin(p.y * 0.02f) * 0.15f
        wrapDown(p)
    }

    private fun updateRain(p: VizParticle) {
        p.y += p.speedY
        if (p.y > height + p.size) respawnTop(p)
    }

    private fun updateFire(p: VizParticle) {
        p.y += p.speedY
        p.x += p.speedX
        p.alpha *= 0.996f
        if (p.y < height * 0.35f || p.alpha < 0.05f) respawnBottom(p)
    }

    private fun updateSand(p: VizParticle) {
        p.x += p.speedX
        p.y += sin(p.x * 0.01f) * 0.2f
        if (p.x > width + p.size) {
            p.x = -p.size
            p.y = Random.nextFloat() * height
        }
    }

    private fun updateLeaf(p: VizParticle) {
        p.y += p.speedY
        p.x += p.speedX + sin(p.y * 0.015f) * 0.5f
        p.rotation += p.rotationSpeed
        wrapDown(p)
    }

    private fun wrapDown(p: VizParticle) {
        if (p.y > height + p.size) respawnTop(p)
        if (p.x < -p.size) p.x = width + p.size
        if (p.x > width + p.size) p.x = -p.size
    }

    private fun respawnTop(p: VizParticle) {
        val fresh = spawn(Random.nextFloat() * width, -p.size)
        p.x = fresh.x; p.y = fresh.y; p.speedY = fresh.speedY
        p.speedX = fresh.speedX; p.alpha = fresh.alpha; p.size = fresh.size
    }

    private fun respawnBottom(p: VizParticle) {
        p.x = width * (0.3f + Random.nextFloat() * 0.4f)
        p.y = height * (0.75f + Random.nextFloat() * 0.2f)
        p.speedY = -(Random.nextFloat() * 1.5f + 0.8f)
        p.alpha = Random.nextFloat() * 0.5f + 0.4f
    }

    private fun DrawScope.drawSoftDot(p: VizParticle) {
        drawCircle(
            color = p.color.copy(alpha = p.alpha),
            radius = p.size,
            center = Offset(p.x, p.y),
        )
    }

    private fun DrawScope.drawRainDrop(p: VizParticle) {
        drawLine(
            color = p.color.copy(alpha = p.alpha),
            start = Offset(p.x, p.y),
            end = Offset(p.x - 2f, p.y + p.size),
            strokeWidth = 1.2f,
        )
    }

    private fun DrawScope.drawEmber(p: VizParticle) {
        drawCircle(
            color = p.color.copy(alpha = p.alpha),
            radius = p.size,
            center = Offset(p.x, p.y),
        )
        drawCircle(
            color = Color(0xFFFFD080).copy(alpha = p.alpha * 0.35f),
            radius = p.size * 2f,
            center = Offset(p.x, p.y),
        )
    }

    private fun DrawScope.drawLeaf(p: VizParticle) {
        rotate(p.rotation, pivot = Offset(p.x, p.y)) {
            drawOval(
                color = p.color.copy(alpha = p.alpha),
                topLeft = Offset(p.x - p.size * 0.4f, p.y - p.size * 0.7f),
                size = Size(p.size * 0.8f, p.size * 1.4f),
            )
        }
    }
}

@Composable
fun CalmingVisualizationCanvas(
    visualizationId: CalmingVisualizationId,
    backgroundTop: Color,
    backgroundBottom: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    particleCount: Int = 280,
) {
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    val system = remember(visualizationId) {
        VisualizationParticleSystem(visualizationId, particleCount)
    }

    LaunchedEffect(isPlaying, canvasWidth, canvasHeight) {
        while (isActive) {
            withFrameNanos {
                system.resize(canvasWidth, canvasHeight)
                system.update(isPlaying)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                canvasWidth = it.width.toFloat()
                canvasHeight = it.height.toFloat()
            },
    ) {
        drawRect(
            brush = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom)),
        )
        if (visualizationId == CalmingVisualizationId.Firepit) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF6B35).copy(alpha = 0.25f),
                        Color(0xFFFF6B35).copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width / 2f, size.height * 0.82f),
                    radius = size.width * 0.35f,
                ),
                radius = size.width * 0.35f,
                center = Offset(size.width / 2f, size.height * 0.82f),
            )
        }
        system.draw(this)
    }
}
