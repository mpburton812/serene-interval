package com.example.meditationparticles.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.model.Particle
import com.example.meditationparticles.model.createParticle
import com.example.meditationparticles.ui.theme.SandBackground
import com.example.meditationparticles.ui.theme.spectrumColor
import com.example.meditationparticles.ui.theme.spectrumGradientColors
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.isActive

private val LabelColor = Color(0xFFE8D4A8)
private val TrackLowColor = Color(0xFF2A2420)
private val TrackHighColor = Color(0xFFE8D4A8)
private val OverlayButtonColor = Color.White.copy(alpha = 0.5f)
private val ControlsPanelColor = Color(0xEE1A1612)

private const val MIN_PARTICLES = 0
private const val MAX_PARTICLES = 2000
private const val DEFAULT_PARTICLES = 500
private const val MIN_DROP_SPEED = 0
private const val MAX_DROP_SPEED = 100
private const val DEFAULT_DROP_SPEED = 10
private const val MAX_SPEED_MULTIPLIER = 10f
private const val DEFAULT_SPECTRUM_HUE = 0.08f

private data class TouchInteraction(
    val position: Offset,
    val pressure: Float,
)

private fun dropSpeedScale(dropSpeed: Int): Float =
    (dropSpeed / MAX_DROP_SPEED.toFloat()) * MAX_SPEED_MULTIPLIER

private fun normalizedValue(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    val span = range.endInclusive - range.start
    if (span == 0f) return 0f
    return ((value - range.start) / span).coerceIn(0f, 1f)
}

private fun normalizePressure(rawPressure: Float): Float {
    if (rawPressure <= 0f) return 0.35f
    return rawPressure.coerceIn(0.2f, 1f)
}

private fun pressureForceMultiplier(pressure: Float): Float {
    val normalized = normalizePressure(pressure)
    return 0.45f + ((normalized - 0.2f) / 0.8f) * 2.55f
}

private fun applyWindForce(
    x: Float,
    y: Float,
    touchX: Float,
    touchY: Float,
    windRadiusPx: Float,
    avoidRadiusPx: Float,
    pressure: Float,
): Offset {
    val dx = x - touchX
    val dy = y - touchY
    val distSq = dx * dx + dy * dy
    if (distSq < 0.001f) {
        val scatter = pressureForceMultiplier(pressure)
        return Offset(
            (Random.nextFloat() * 2f - 1f) * scatter,
            -0.5f * scatter,
        )
    }

    val dist = sqrt(distSq)
    if (dist >= windRadiusPx) return Offset.Zero

    val normalized = 1f - (dist / windRadiusPx)
    val strength = normalized * normalized
    val avoidBoost = if (dist < avoidRadiusPx) {
        1f + (1f - dist / avoidRadiusPx) * 1.5f
    } else {
        1f
    }

    val push = strength * avoidBoost * 3.5f * pressureForceMultiplier(pressure)
    val nx = dx / dist
    val ny = dy / dist

    return Offset(nx * push, ny * push * 0.35f)
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    drawIcon: DrawScope.(radius: Float, center: Offset) -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = OverlayButtonColor,
                radius = radius,
                center = center,
            )
            drawIcon(radius, center)
        }
    }
}

@Composable
private fun ExitButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CircleIconButton(onClick = onClick, modifier = modifier) { radius, center ->
        val arm = radius * 0.38f
        val stroke = radius * 0.14f
        drawLine(
            color = OverlayButtonColor,
            start = Offset(center.x - arm, center.y - arm),
            end = Offset(center.x + arm, center.y + arm),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = OverlayButtonColor,
            start = Offset(center.x + arm, center.y - arm),
            end = Offset(center.x - arm, center.y + arm),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun MenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CircleIconButton(onClick = onClick, modifier = modifier) { radius, center ->
        val lineWidth = radius * 0.72f
        val stroke = radius * 0.11f
        val spacing = radius * 0.28f
        listOf(-spacing, 0f, spacing).forEach { offsetY ->
            drawLine(
                color = OverlayButtonColor,
                start = Offset(center.x - lineWidth / 2f, center.y + offsetY),
                end = Offset(center.x + lineWidth / 2f, center.y + offsetY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    trackBrush: Brush,
    thumbColor: Color,
    labelStart: String,
    labelCenter: String,
    labelEnd: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = labelStart, color = LabelColor)
            Text(text = labelCenter, color = LabelColor)
            Text(text = labelEnd, color = LabelColor)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(trackBrush),
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
fun FallingParticlesScreen() {
    val density = LocalDensity.current
    val activity = LocalContext.current as ComponentActivity
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    var canvasHeight by remember { mutableFloatStateOf(0f) }
    var particleCount by remember { mutableIntStateOf(DEFAULT_PARTICLES) }
    var dropSpeed by remember { mutableIntStateOf(DEFAULT_DROP_SPEED) }
    var spectrumHue by remember { mutableFloatStateOf(DEFAULT_SPECTRUM_HUE) }
    var touchInteraction by remember { mutableStateOf<TouchInteraction?>(null) }
    var controlsExpanded by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<Particle>() }
    val currentDropSpeed by rememberUpdatedState(dropSpeed)
    val currentSpectrumHue by rememberUpdatedState(spectrumHue)
    val currentTouchInteraction by rememberUpdatedState(touchInteraction)

    val windRadiusPx = with(density) { 110.dp.toPx() }
    val avoidRadiusPx = with(density) { 44.dp.toPx() }

    val particleRange = MIN_PARTICLES.toFloat()..MAX_PARTICLES.toFloat()
    val speedRange = MIN_DROP_SPEED.toFloat()..MAX_DROP_SPEED.toFloat()
    val colorRange = 0f..1f

    LaunchedEffect(particleCount, canvasWidth, canvasHeight) {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return@LaunchedEffect

        val baseColor = spectrumColor(spectrumHue)
        while (particles.size < particleCount) {
            particles.add(createParticle(canvasWidth, canvasHeight, density, baseColor))
        }
        while (particles.size > particleCount) {
            particles.removeAt(particles.lastIndex)
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos {
                if (particles.isEmpty() || canvasHeight <= 0f) return@withFrameNanos

                val speedScale = dropSpeedScale(currentDropSpeed)
                val touch = currentTouchInteraction
                if (speedScale == 0f && touch == null) return@withFrameNanos

                for (i in particles.indices) {
                    val particle = particles[i]
                    var newY = particle.y
                    var newX = particle.x
                    var newRotation = particle.rotation

                    if (speedScale > 0f) {
                        newY += particle.speedPxPerFrame * speedScale
                        newX += particle.driftPxPerFrame * speedScale
                        newRotation += particle.rotationSpeed * speedScale
                    }

                    if (touch != null) {
                        val wind = applyWindForce(
                            x = newX,
                            y = newY,
                            touchX = touch.position.x,
                            touchY = touch.position.y,
                            windRadiusPx = windRadiusPx,
                            avoidRadiusPx = avoidRadiusPx,
                            pressure = touch.pressure,
                        )
                        newX += wind.x
                        newY += wind.y
                    }

                    if (newX < -particle.radiusX) newX = canvasWidth + particle.radiusX
                    if (newX > canvasWidth + particle.radiusX) newX = -particle.radiusX

                    if (newY - particle.radiusY > canvasHeight) {
                        newY = -particle.radiusY
                        newX = Random.nextFloat() * canvasWidth
                        newRotation = Random.nextFloat() * 360f
                    }

                    particles[i] = particle.copy(x = newX, y = newY, rotation = newRotation)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SandBackground),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasWidth = it.width.toFloat()
                    canvasHeight = it.height.toFloat()
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        controlsExpanded = false
                        touchInteraction = TouchInteraction(
                            position = down.position,
                            pressure = normalizePressure(down.pressure),
                        )
                        val pointerId = down.id
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            touchInteraction = TouchInteraction(
                                position = change.position,
                                pressure = normalizePressure(change.pressure),
                            )
                        } while (true)
                        touchInteraction = null
                    }
                },
        ) {
            val drawColor = spectrumColor(currentSpectrumHue)
            particles.forEach { particle ->
                rotate(degrees = particle.rotation, pivot = Offset(particle.x, particle.y)) {
                    drawOval(
                        color = drawColor.copy(alpha = particle.color.alpha),
                        topLeft = Offset(
                            particle.x - particle.radiusX,
                            particle.y - particle.radiusY,
                        ),
                        size = Size(particle.radiusX * 2f, particle.radiusY * 2f),
                    )
                }
            }
        }

        ExitButton(
            onClick = { activity.finish() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp),
        )

        MenuButton(
            onClick = { controlsExpanded = !controlsExpanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 12.dp),
        )

        AnimatedVisibility(
            visible = controlsExpanded,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
            ) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ControlsPanelColor)
                    .navigationBarsPadding()
                    .padding(top = 12.dp, bottom = 72.dp),
            ) {
                StyledSlider(
                    value = spectrumHue,
                    onValueChange = { spectrumHue = it },
                    valueRange = colorRange,
                    trackBrush = Brush.horizontalGradient(spectrumGradientColors),
                    thumbColor = spectrumColor(spectrumHue),
                    labelStart = "Red",
                    labelCenter = "Color",
                    labelEnd = "Violet",
                )

                StyledSlider(
                    value = particleCount.toFloat(),
                    onValueChange = {
                        particleCount = it.roundToInt().coerceIn(MIN_PARTICLES, MAX_PARTICLES)
                    },
                    valueRange = particleRange,
                    trackBrush = Brush.horizontalGradient(listOf(TrackLowColor, TrackHighColor)),
                    thumbColor = lerp(
                        TrackLowColor,
                        TrackHighColor,
                        normalizedValue(particleCount.toFloat(), particleRange),
                    ),
                    labelStart = "0",
                    labelCenter = "$particleCount particles",
                    labelEnd = "2000",
                )

                StyledSlider(
                    value = dropSpeed.toFloat(),
                    onValueChange = {
                        dropSpeed = it.roundToInt().coerceIn(MIN_DROP_SPEED, MAX_DROP_SPEED)
                    },
                    valueRange = speedRange,
                    trackBrush = Brush.horizontalGradient(listOf(TrackLowColor, TrackHighColor)),
                    thumbColor = lerp(
                        TrackLowColor,
                        TrackHighColor,
                        normalizedValue(dropSpeed.toFloat(), speedRange),
                    ),
                    labelStart = "0",
                    labelCenter = if (dropSpeed == 0) "Speed: stopped" else "Speed: $dropSpeed",
                    labelEnd = "100",
                )
            }
        }
    }
}
