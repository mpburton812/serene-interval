package com.safehaven.affirmations.widget

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import com.safehaven.affirmations.R
import com.safehaven.affirmations.domain.mood.MoodScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MoodLogAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val level = parameters[moodLevelKey] ?: return
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        // Ignore duplicate taps while bounce/flash is in progress (matches in-app disabled buttons).
        if (MoodWidgetFlashStore.isActive(appWidgetId)) return

        val appContext = context.applicationContext
        val insertId = try {
            withContext(Dispatchers.IO) {
                MoodWidgetLogHandler.fromContext(appContext).logLevel(level)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to record widget mood level=$level", error)
            showToast(appContext, appContext.getString(R.string.mood_widget_log_failed_toast))
            return
        }

        if (insertId == null) {
            Log.w(TAG, "Mood widget record returned null for level=$level")
            showToast(appContext, appContext.getString(R.string.mood_widget_log_failed_toast))
            return
        }

        showToast(appContext, appContext.getString(R.string.mood_widget_logged_toast))

        val migratedLevel = MoodScale.migrateFromLegacy(level)
        val colorArgb = MoodScale.colorArgb(migratedLevel)
        val widget = MoodWidget()
        val startedAtMillis = System.currentTimeMillis()
        try {
            MoodWidgetSelectionFlash.run { alpha, enlarged ->
                MoodWidgetFlashStore.set(
                    appWidgetId,
                    if (alpha > 0f || enlarged) {
                        MoodWidgetFlash(
                            colorArgb = colorArgb,
                            alpha = alpha,
                            level = migratedLevel,
                            enlarged = enlarged,
                            startedAtMillis = startedAtMillis,
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

    private fun showToast(context: Context, message: String) {
        // ActionCallback runs off the main thread; Toast requires main.
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MoodLogAction"
        val moodLevelKey = ActionParameters.Key<Int>("mood_level")
    }
}
