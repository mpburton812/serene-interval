package com.example.meditationparticles.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.example.meditationparticles.R
import com.example.meditationparticles.ui.theme.SereneIntervalTheme
import com.example.meditationparticles.ui.theme.SereneSpacing
import kotlinx.coroutines.launch

class MoodWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val preferences = MoodWidgetPreferences(this)
        val initialConfig = preferences.load(appWidgetId)

        setContent {
            SereneIntervalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MoodWidgetConfigureScreen(
                        initialConfig = initialConfig,
                        onSave = { config ->
                            preferences.save(appWidgetId, config)
                            lifecycleScope.launch {
                                val glanceId = GlanceAppWidgetManager(this@MoodWidgetConfigureActivity)
                                    .getGlanceIdBy(appWidgetId)
                                MoodWidget().update(this@MoodWidgetConfigureActivity, glanceId)
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                                )
                                finish()
                            }
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MoodWidgetConfigureScreen(
    initialConfig: MoodWidgetConfig,
    onSave: (MoodWidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    var backgroundStyle by remember { mutableStateOf(initialConfig.backgroundStyle) }
    var transparency by remember { mutableFloatStateOf(initialConfig.transparency) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        Text(
            text = stringResource(R.string.mood_widget_configure_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.mood_widget_configure_background),
            style = MaterialTheme.typography.titleMedium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MoodWidgetBackgroundStyle.entries.forEachIndexed { index, style ->
                SegmentedButton(
                    selected = backgroundStyle == style,
                    onClick = { backgroundStyle = style },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = MoodWidgetBackgroundStyle.entries.size,
                    ),
                ) {
                    Text(
                        text = when (style) {
                            MoodWidgetBackgroundStyle.WHITE ->
                                stringResource(R.string.mood_widget_background_white)
                            MoodWidgetBackgroundStyle.BLACK ->
                                stringResource(R.string.mood_widget_background_black)
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.mood_widget_configure_transparency),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = transparency,
            onValueChange = { transparency = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.mood_widget_configure_transparency_value,
                (transparency * 100).toInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        ) {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.mood_widget_configure_cancel))
            }
            Button(
                onClick = {
                    onSave(
                        MoodWidgetConfig(
                            backgroundStyle = backgroundStyle,
                            transparency = transparency,
                        ),
                    )
                },
            ) {
                Text(text = stringResource(R.string.mood_widget_configure_save))
            }
        }
    }
}
