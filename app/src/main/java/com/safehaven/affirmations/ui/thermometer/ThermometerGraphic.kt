package com.safehaven.affirmations.ui.thermometer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.domain.thermometer.ThermometerRules

fun thermometerColor(score: Int): Color {
    val t = ThermometerRules.clampScore(score) / ThermometerRules.MAX_SCORE.toFloat()
    return Color(
        red = 0.15f + 0.85f * t,
        green = 0.35f * (1f - t),
        blue = 0.95f * (1f - t),
    )
}

@Composable
fun ThermometerGraphic(
    score: Int?,
    modifier: Modifier = Modifier,
) {
    val fill = score?.let(::thermometerColor) ?: Color(0xFFB0BEC5)
    Canvas(
        modifier = modifier
            .width(56.dp)
            .height(160.dp),
    ) {
        val stemWidth = size.width * 0.34f
        val bulbRadius = size.width * 0.38f
        val stemLeft = (size.width - stemWidth) / 2f
        val stemTop = size.height * 0.06f
        val stemBottom = size.height - bulbRadius * 1.35f
        val bulbCenter = Offset(size.width / 2f, size.height - bulbRadius * 0.95f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(stemLeft, stemTop),
            size = Size(stemWidth, stemBottom - stemTop),
            cornerRadius = CornerRadius(stemWidth / 2f, stemWidth / 2f),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = bulbRadius,
            center = bulbCenter,
        )

        val fraction = (score ?: 0) / ThermometerRules.MAX_SCORE.toFloat()
        val mercuryTop = stemBottom - (stemBottom - stemTop - stemWidth) * fraction
        if (score != null) {
            drawRoundRect(
                color = fill,
                topLeft = Offset(stemLeft + stemWidth * 0.22f, mercuryTop),
                size = Size(stemWidth * 0.56f, (stemBottom - mercuryTop).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(stemWidth / 4f, stemWidth / 4f),
            )
            drawCircle(
                color = fill,
                radius = bulbRadius * 0.72f,
                center = bulbCenter,
            )
        } else {
            drawCircle(
                color = Color(0xFFCFD8DC),
                radius = bulbRadius * 0.72f,
                center = bulbCenter,
            )
        }
    }
}
