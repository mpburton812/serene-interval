package com.safehaven.affirmations.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.safehaven.affirmations.R
import com.safehaven.affirmations.domain.mood.MoodScale

class MoodLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val level = parameters[moodLevelKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        if (MoodWidgetFlashStore.isActive(appWidgetId)) return

        val colorArgb = MoodScale.colorArgb(MoodScale.migrateFromLegacy(level))
        val migratedLevel = MoodScale.migrateFromLegacy(level)
        val widget = MoodWidget()
        try {
            MoodWidgetSelectionFlash.run { frame ->
                MoodWidgetFlashStore.set(
                    appWidgetId,
                    MoodWidgetFlash(
                        colorArgb = colorArgb,
                        alpha = frame.backgroundAlpha,
                        level = migratedLevel,
                        faceEnlarged = frame.faceEnlarged,
                    ),
                )
                widget.update(context, glanceId)
            }

            MoodWidgetLogHandler.fromContext(context).logLevel(level)
            Toast.makeText(
                context.applicationContext,
                context.getString(R.string.mood_widget_logged_toast),
                Toast.LENGTH_SHORT,
            ).show()
        } finally {
            MoodWidgetFlashStore.clear(appWidgetId)
            widget.update(context, glanceId)
        }
    }

    companion object {
        val moodLevelKey = ActionParameters.Key<Int>("mood_level")
    }
}
