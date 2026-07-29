package com.example.meditationparticles.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.meditationparticles.R
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.delay

class MoodLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val level = parameters[moodLevelKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val preferences = MoodWidgetPreferences(context)
        val widget = MoodWidget()

        MoodWidgetLogHandler.fromContext(context).logLevel(level)
        Toast.makeText(
            context.applicationContext,
            context.getString(R.string.mood_widget_logged_toast),
            Toast.LENGTH_SHORT,
        ).show()

        val stepDelayMs = MoodWidgetFlash.stepDelayMs()
        repeat(MoodWidgetFlash.PULSE_COUNT) {
            MoodWidgetFlash.fadeInSteps().forEach { blend ->
                preferences.saveFlash(appWidgetId, MoodWidgetFlashState(level = level, blend = blend))
                widget.update(context, glanceId)
                delay(stepDelayMs)
            }
            MoodWidgetFlash.fadeOutSteps().forEach { blend ->
                preferences.saveFlash(appWidgetId, MoodWidgetFlashState(level = level, blend = blend))
                widget.update(context, glanceId)
                delay(stepDelayMs)
            }
        }
        preferences.clearFlash(appWidgetId)
        widget.update(context, glanceId)
    }

    companion object {
        val moodLevelKey = ActionParameters.Key<Int>("mood_level")
    }
}
