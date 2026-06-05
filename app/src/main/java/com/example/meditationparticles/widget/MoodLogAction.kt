package com.example.meditationparticles.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import com.example.meditationparticles.R
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

class MoodLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val level = parameters[moodLevelKey] ?: return
        MoodWidgetLogHandler.fromContext(context).logLevel(level)
        Toast.makeText(
            context.applicationContext,
            context.getString(R.string.mood_widget_logged_toast),
            Toast.LENGTH_SHORT,
        ).show()
        MoodWidget().update(context, glanceId)
    }

    companion object {
        val moodLevelKey = ActionParameters.Key<Int>("mood_level")
    }
}
