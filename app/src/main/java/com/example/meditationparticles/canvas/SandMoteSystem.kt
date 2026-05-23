package com.example.meditationparticles.canvas

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class SandMote(
    val index: Int,
    val size: Float,
    var angle: Float,
    val angleSpeed: Float,
    val orbitRadiusFactor: Float,
    var x: Float = 0f,
    var y: Float = 0f,
)

/**
 * Motes live only inside the currently filling sphere — never in pipes.
 */
class SandMoteSystem(
    private val moteCount: Int = 36,
) {
    val motes: List<SandMote> = List(moteCount) { index ->
        SandMote(
            index = index,
            size = Random.nextFloat() * 2f + 2f,
            angle = Random.nextFloat() * 6.28f,
            angleSpeed = Random.nextFloat() * 0.035f + 0.02f,
            orbitRadiusFactor = Random.nextFloat() * 0.45f + 0.15f,
        )
    }

    private var boundSphereId: Int = -1

    fun update(
        activeSphere: BreathSphere?,
        fillLevel: Float,
        timeMs: Long,
        scale: Float,
    ) {
        if (activeSphere == null || fillLevel <= 0f) {
            boundSphereId = -1
            return
        }

        if (boundSphereId != activeSphere.id) {
            boundSphereId = activeSphere.id
            motes.forEach { mote ->
                mote.angle = Random.nextFloat() * 6.28f
            }
        }

        val boundRadius = activeSphere.radius * 0.72f
        val s = scale.coerceIn(0.8f, 1.35f)
        val visibleCount = (moteCount * fillLevel.coerceIn(0.08f, 1f)).toInt().coerceAtLeast(4)

        motes.forEachIndexed { index, mote ->
            if (index >= visibleCount) return@forEachIndexed

            mote.angle += mote.angleSpeed
            val wobble = sin(timeMs / 220f + mote.index * 0.9f) * boundRadius * 0.08f
            val r = boundRadius * mote.orbitRadiusFactor + wobble
            mote.x = activeSphere.center.x + cos(mote.angle) * r
            mote.y = activeSphere.center.y + sin(mote.angle) * r * 0.92f

            // Keep inside sphere
            val dx = mote.x - activeSphere.center.x
            val dy = mote.y - activeSphere.center.y
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            val maxDist = boundRadius * 0.95f
            if (dist > maxDist && dist > 0f) {
                mote.x = activeSphere.center.x + dx / dist * maxDist
                mote.y = activeSphere.center.y + dy / dist * maxDist
            }
        }
    }

    fun visibleMoteCount(fillLevel: Float): Int =
        if (fillLevel <= 0f) 0 else (moteCount * fillLevel.coerceIn(0.08f, 1f)).toInt().coerceAtLeast(4)
}
