package com.example.loglife.ui.widgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.loglife.ui.logging.LogEventScreen
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.main.MainViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Simple activity to host the LogEventScreen for widget launches.
 */
@AndroidEntryPoint
class LogEventActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoglifeTheme {
                LogEventScreen(
                    viewModel = viewModel,
                    onEventSelected = { eventTypeId ->
                        // Handle event selection from widget
                        // For instant events: log immediately
                        // For duration events: start the event
                        // Then finish this activity
                        finish()
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}