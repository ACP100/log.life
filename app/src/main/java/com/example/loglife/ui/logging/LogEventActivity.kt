package com.example.loglife.ui.logging

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.logging.LogEventScreen

/**
 * Activity for hosting the Log Event screen.
 * This activity is launched from the widget to allow quick event logging.
 */
@AndroidEntryPoint
class LogEventActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoglifeTheme {
                LogEventScreen(
                    viewModel = viewModel,
                    onEventSelected = { eventTypeId ->
                        viewModel.logEvent(eventTypeId)
                        finish()
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
