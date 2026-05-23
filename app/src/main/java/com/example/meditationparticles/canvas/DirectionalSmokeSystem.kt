package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import com.example.meditationparticles.domain.breathing.FillDirection
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SmokeParticle(
    val index: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val baseSize: Float,
    var life: Float,
    var alpha: Float,
)

/**
 * Directional smoke particles spawn at the fill front and drift with soft wobble.
 * Particles stay inside the active sphere only — never in pipes.
 */
class DirectionalSmokeSystem(
    private val maxParticles: Int = 48,
) {
    private val particles = List(maxParticles) { index ->
        SmokeParticle(
            index = index,
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            baseSize = Random.nextFloat() * 2.5f + 2f,
            life = 0f,
            alpha = 0f,
        )
    }

    private var boundSphereId: Int = -1
    private var spawnAccumulator = 0f

    fun update(
        activeSphere: BreathSphere?,
        fillLevel: Float,
        fillDirection: FillDirection,
        timeMs: Long,
        scale: Float,
    ) {
        if (activeSphere == null || fillLevel <= 0f) {
            boundSphereId = -1
            spawnAccumulator = 0f
            particles.forEach { it.life = 0f; it.alpha = 0f }
            return
        }

        if (boundSphereId != activeSphere.id) {
            boundSphereId = activeSphere.id
            spawnAccumulator = 0f
            particles.forEach { it.life = 0f; it.alpha = 0f }
        }

        val radius = activeSphere.radius * 0.88f
        val cx = activeSphere.center.x
        val cy = activeSphere.center.y
        val s = scale.coerceIn(0.8f, 1.35f)
        val fill = fillLevel.coerceIn(0.05f, 1f)

        val fillFrontY = when (fillDirection) {
            FillDirection.BottomToTop, FillDirection.BottomToTopHold -> cy + radius - fill * radius * 2f
            FillDirection.TopToBottom -> cy - radius + fill * radius * 2f
        }

        val spawnRate = (4f + fill * 12f) * s
        spawnAccumulator += spawnRate / 60f
        while (spawnAccumulator >= 1f) {
            spawnAccumulator -= 1f
            spawnParticle(cx, cy, radius, fillFrontY, fillDirection, timeMs, s)
        }

        particles.forEach { p ->
            if (p.life <= 0f) return@forEach

            val wobble = sin(timeMs / 180f + p.index * 1.3f) * 0.35f
            p.vx = wobble * s
            p.vy += when (fillDirection) {
                FillDirection.BottomToTop, FillDirection.BottomToTopHold -> -0.18f * s
                FillDirection.TopToBottom -> 0.18f * s
            }
            p.x += p.vx
            p.y += p.vy
            p.life -= 0.012f
            p.alpha = (p.life * 0.85f).coerceIn(0f, 0.9f)

            val dx = p.x - cx
            val dy = p.y - cy
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist > radius * 0.92f && dist > 0f) {
                p.x = cx + dx / dist * radius * 0.9f
                p.y = cy + dy / dist * radius * 0.9f
                p.life *= 0.85f
            }
        }
    }

    private fun spawnParticle(
        cx: Float,
        cy: Float,
        radius: Float,
        fillFrontY: Float,
        direction: FillDirection,
        timeMs: Long,
        scale: Float,
    ) {
        val slot = particles.minByOrNull { it.life } ?: return
        val spreadX = radius * 0.55f
        slot.x = cx + (Random.nextFloat() - 0.5f) * spreadX
        slot.y = fillFrontY + (Random.nextFloat() - 0.5f) * radius * 0.12f
        slot.vx = (Random.nextFloat() - 0.5f) * 0.4f * scale
        slot.vy = when (direction) {
            FillDirection.BottomToTop, FillDirection.BottomToTopHold -> -0.25f * scale
            FillDirection.TopToBottom -> 0.25f * scale
        }
        slot.life = Random.nextFloat() * 0.35f + 0.55f
        slot.alpha = 0.7f
    }

    fun activeParticles(): List<SmokeParticle> = particles.filter { it.life > 0f && it.alpha > 0.02f }
}
