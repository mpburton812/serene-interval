package com.example.meditationparticles.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.mood.MoodScale

class MoodWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config = MoodWidgetPreferences(context).load(appWidgetId)
        val flash = MoodWidgetFlashStore.get(appWidgetId)
        provideContent {
            MoodWidgetContent(config = config, flash = flash)
        }
    }
}

@Composable
internal fun MoodWidgetContent(
    config: MoodWidgetConfig,
    flash: MoodWidgetFlash? = null,
) {
    val baseArgb = when (config.backgroundStyle) {
        MoodWidgetBackgroundStyle.WHITE -> 0xFFFFFFFF
        MoodWidgetBackgroundStyle.BLACK -> 0xFF000000
    }
    val backgroundColor = resolveWidgetBackground(
        baseArgb = baseArgb,
        transparency = config.transparency,
        flash = flash,
    )
    val levels = MoodWidgetLayout.moodLevels()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(backgroundColor))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            levels.forEach { level ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    MoodWidgetButton(level = level, flash = flash)
                }
            }
        }
    }
}

internal fun resolveWidgetBackground(
    baseArgb: Long,
    transparency: Float,
    flash: MoodWidgetFlash?,
): Color {
    if (flash != null && flash.alpha > 0f) {
        val flashStrength = flash.alpha.coerceIn(0f, 1f)
        val flashColor = Color(flash.colorArgb).copy(alpha = transparency * flashStrength)
        if (flashStrength >= 0.999f) {
            return flashColor
        }
        // Cross-fade base chrome with the selected mood color for intermediate frames.
        val base = Color(baseArgb).copy(alpha = transparency * (1f - flashStrength))
        return Color(
            red = base.red * (1f - flashStrength) + flashColor.red * flashStrength,
            green = base.green * (1f - flashStrength) + flashColor.green * flashStrength,
            blue = base.blue * (1f - flashStrength) + flashColor.blue * flashStrength,
            alpha = (base.alpha * (1f - flashStrength) + flashColor.alpha * flashStrength)
                .coerceIn(0f, 1f),
        )
    }
    return Color(baseArgb).copy(alpha = transparency)
}

@Composable
private fun MoodWidgetButton(level: Int, flash: MoodWidgetFlash?) {
    val color = Color(MoodScale.colorArgb(level))
    val context = LocalContext.current
    val enlarged = MoodWidgetSelectionFlash.isFaceEnlarged(flash, level)
    val circleDp = MoodWidgetSelectionFlash.circleSizeDp(enlarged)
    val iconDp = MoodWidgetSelectionFlash.iconSizeDp(enlarged)
    val cornerDp = MoodWidgetSelectionFlash.cornerRadiusDp(enlarged)

    Box(
        modifier = GlanceModifier
            .size(circleDp.dp)
            .background(ColorProvider(color.copy(alpha = 0.18f)))
            .cornerRadius(cornerDp.dp)
            .clickable(
                onClick = actionRunCallback<MoodLogAction>(
                    actionParametersOf(MoodLogAction.moodLevelKey to level),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(moodDrawableRes(level)),
            contentDescription = context.getString(
                R.string.mood_widget_button_description,
                MoodScale.label(level),
            ),
            modifier = GlanceModifier.size(iconDp.dp),
            colorFilter = ColorFilter.tint(ColorProvider(color)),
        )
    }
}

private fun moodDrawableRes(level: Int): Int = when (MoodScale.migrateFromLegacy(level)) {
    1 -> R.drawable.ic_mood_very_dissatisfied
    2 -> R.drawable.ic_mood_dissatisfied
    3 -> R.drawable.ic_mood_neutral
    4 -> R.drawable.ic_mood_very_satisfied
    else -> R.drawable.ic_mood_very_satisfied
}
