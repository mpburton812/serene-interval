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
        provideContent {
            MoodWidgetContent(config = config)
        }
    }
}

@Composable
private fun MoodWidgetContent(config: MoodWidgetConfig) {
    val backgroundArgb = when (config.backgroundStyle) {
        MoodWidgetBackgroundStyle.WHITE -> 0xFFFFFFFF
        MoodWidgetBackgroundStyle.BLACK -> 0xFF000000
    }
    val backgroundColor = Color(backgroundArgb).copy(alpha = config.transparency)
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
                    MoodWidgetButton(level = level)
                }
            }
        }
    }
}

@Composable
private fun MoodWidgetButton(level: Int) {
    val color = Color(MoodScale.colorArgb(level))
    val context = LocalContext.current

    Box(
        modifier = GlanceModifier
            .size(48.dp)
            .background(ColorProvider(color.copy(alpha = 0.18f)))
            .cornerRadius(24.dp)
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
            modifier = GlanceModifier.size(32.dp),
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
