package com.example.meditationparticles.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.example.meditationparticles.R
import com.example.meditationparticles.domain.mood.MoodScale

class MoodLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val level = parameters[moodLevelKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        if (MoodWidgetFlashStore.isActive(appWidgetId)) return

        MoodWidgetLogHandler.fromContext(context).logLevel(level)
        Toast.makeText(
            context.applicationContext,
            context.getString(R.string.mood_widget_logged_toast),
            Toast.LENGTH_SHORT,
        ).show()

        val colorArgb = MoodScale.colorArgb(MoodScale.migrateFromLegacy(level))
        val migratedLevel = MoodScale.migrateFromLegacy(level)
        val widget = MoodWidget()
        try {
            MoodWidgetSelectionFlash.run { alpha ->
                MoodWidgetFlashStore.set(
                    appWidgetId,
                    if (alpha > 0f) {
                        MoodWidgetFlash(
                            colorArgb = colorArgb,
                            alpha = alpha,
                            level = migratedLevel,
                        )
                    } else {
                        null
                    },
                )
                widget.update(context, glanceId)
            }
        } finally {
            MoodWidgetFlashStore.clear(appWidgetId)
            widget.update(context, glanceId)
        }
    }

    companion object {
        val moodLevelKey = ActionParameters.Key<Int>("mood_level")
    }
}
