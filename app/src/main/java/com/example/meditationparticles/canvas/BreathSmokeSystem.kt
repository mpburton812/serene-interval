package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import com.example.meditationparticles.domain.breathing.FillDirection
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class SmokeParticle(
    val index: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var life: Float,
    var alpha: Float,
    var sphereId: Int = -1,
)

data class SmokeTarget(
    val sphere: BreathSphere,
    val fillLevel: Float,
    val fillDirection: FillDirection,
    val isDraining: Boolean,
)

/**
 * Dynamic smoke wisps spawn at fill/drain interfaces and swirl inside active spheres.
 */
class BreathSmokeSystem(
    private val maxParticles: Int = 160,
) {
    private val particles = List(maxParticles) { index ->
        SmokeParticle(
            index = index,
            x = 0f,
            y = 0f,
            vx = 0f,
            vy = 0f,
            size = Random.nextFloat() * 2.8f + 2.4f,
            life = 0f,
            alpha = 0f,
        )
    }

    private val spawnAccumulators = mutableMapOf<Int, Float>()

    fun update(
        targets: List<SmokeTarget>,
        timeMs: Long,
        scale: Float,
    ) {
        val activeIds = targets.map { it.sphere.id }.toSet()
        spawnAccumulators.keys.retainAll(activeIds)
        targets.forEach { target ->
            if (!spawnAccumulators.containsKey(target.sphere.id)) {
                spawnAccumulators[target.sphere.id] = 0f
            }
        }

        if (targets.isEmpty()) {
            particles.forEach { it.life = 0f; it.alpha = 0f }
            spawnAccumulators.clear()
            return
        }

        val s = scale.coerceIn(0.8f, 1.35f)
        targets.forEach { target ->
            updateTargetSpawns(target, timeMs, s)
        }

        particles.forEach { particle ->
            if (particle.life <= 0f) return@forEach
            val target = targets.find { it.sphere.id == particle.sphereId } ?: run {
                particle.life = 0f
                return@forEach
            }

            val radius = target.sphere.radius * 0.9f
            val cx = target.sphere.center.x
            val cy = target.sphere.center.y
            val t = timeMs / 1000f + particle.index * 0.17f
            val curl = sin(t * 4.2f + particle.index) * 0.55f
            val swirl = kotlin.math.cos(t * 3.1f + particle.index * 0.8f) * 0.35f

            particle.vx += curl * 0.04f * s
            particle.vy += when (target.fillDirection) {
                FillDirection.BottomToTop, FillDirection.BottomToTopHold ->
                    if (target.isDraining) 0.12f * s else -0.08f * s
                FillDirection.TopToBottom ->
                    if (target.isDraining) -0.12f * s else 0.08f * s
            }
            particle.vx += swirl * 0.03f * s
            particle.vy += sin(t * 5.5f) * 0.025f * s

            particle.vx *= 0.94f
            particle.vy *= 0.94f
            particle.x += particle.vx
            particle.y += particle.vy

            val decay = if (target.isDraining) 0.016f else 0.011f
            particle.life -= decay
            particle.alpha = (particle.life * (if (target.isDraining) 0.75f else 0.95f)).coerceIn(0f, 0.95f)
            particle.size *= 1.0015f

            val dx = particle.x - cx
            val dy = particle.y - cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > radius && dist > 0f) {
                particle.x = cx + dx / dist * radius * 0.96f
                particle.y = cy + dy / dist * radius * 0.96f
                particle.life *= 0.82f
            }
        }
    }

    private fun updateTargetSpawns(target: SmokeTarget, timeMs: Long, scale: Float) {
        val fill = target.fillLevel.coerceIn(0.04f, 0.995f)
        if (fill <= 0.05f) return

        val radius = target.sphere.radius * 0.88f
        val cx = target.sphere.center.x
        val interfaceY = fillInterfaceY(target.sphere.center, radius, fill, target.fillDirection)

        val baseRate = if (target.isDraining) 10f else 16f
        val spawnRate = (baseRate + fill * 18f) * scale
        var accumulator = spawnAccumulators.getOrPut(target.sphere.id) { 0f } + spawnRate / 60f
        while (accumulator >= 1f) {
            accumulator -= 1f
            spawnParticle(
                sphereId = target.sphere.id,
                cx = cx,
                radius = radius,
                interfaceY = interfaceY,
                target = target,
                timeMs = timeMs,
                scale = scale,
            )
        }
        spawnAccumulators[target.sphere.id] = accumulator
    }

    private fun spawnParticle(
        sphereId: Int,
        cx: Float,
        radius: Float,
        interfaceY: Float,
        target: SmokeTarget,
        timeMs: Long,
        scale: Float,
    ) {
        val slot = particles.minByOrNull { it.life } ?: return
        val spreadX = radius * 0.62f
        slot.sphereId = sphereId
        slot.x = cx + (Random.nextFloat() - 0.5f) * spreadX
        slot.y = interfaceY + (Random.nextFloat() - 0.5f) * radius * 0.14f
        slot.size = Random.nextFloat() * 2.8f + 2.2f
        slot.vx = (Random.nextFloat() - 0.5f) * 0.55f * scale
        slot.vy = when (target.fillDirection) {
            FillDirection.BottomToTop, FillDirection.BottomToTopHold ->
                if (target.isDraining) 0.35f * scale else -0.32f * scale
            FillDirection.TopToBottom ->
                if (target.isDraining) -0.35f * scale else 0.32f * scale
        }
        slot.vx += sin(timeMs / 220f + slot.index) * 0.18f * scale
        slot.life = Random.nextFloat() * 0.45f + (if (target.isDraining) 0.35f else 0.55f)
        slot.alpha = if (target.isDraining) 0.55f else 0.78f
    }

    fun activeParticles(): List<SmokeParticle> = particles.filter { it.life > 0.02f && it.alpha > 0.02f }
}

fun collectSmokeTargets(
    visuals: Map<Int, SphereVisualState>,
    layout: BreathStructureLayout,
): List<SmokeTarget> {
    return layout.allSpheres.mapNotNull { sphere ->
        val visual = visuals[sphere.id] ?: return@mapNotNull null
        if (!visual.isActive || visual.fillLevel <= 0.01f) return@mapNotNull null
        SmokeTarget(
            sphere = sphere,
            fillLevel = visual.fillLevel,
            fillDirection = visual.fillDirection,
            isDraining = visual.isDraining,
        )
    }
}
