package com.example.loglife.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.loglife.ui.theme.LoglifeTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Configuration screen for the event log widget.
 * Lets the user pick a widget mode, persists it, and refreshes the widget.
 * Plain static UI: no ViewModel, so it is safe in Compose Preview.
 */
@AndroidEntryPoint
class EventLogWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setContent {
            LoglifeTheme {
                WidgetConfigureContent(
                    onConfirm = { mode ->
                        EventLogWidgetProvider().saveWidgetMode(this, appWidgetId, mode)
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, 0)
                        val resultValue = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}

private val WidgetModes = listOf(
    EventLogWidgetProvider.WIDGET_MODE_CURRENT_LOG to "Current log",
    EventLogWidgetProvider.WIDGET_MODE_ADD_LOG to "Quick log",
    EventLogWidgetProvider.WIDGET_MODE_BOTH to "Both"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigureContent(
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(EventLogWidgetProvider.WIDGET_MODE_BOTH) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Configure widget") }) }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Choose what this widget shows:",
                style = MaterialTheme.typography.bodyLarge
            )
            WidgetModes.forEach { (mode, label) ->
                Button(
                    onClick = { selectedMode = mode },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedMode != mode
                ) {
                    Text(if (selectedMode == mode) "✓ $label" else label)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(selectedMode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add widget")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun WidgetConfigureContentPreview() {
    LoglifeTheme {
        WidgetConfigureContent(onConfirm = {})
    }
}
