package com.example.loglife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.ui.logging.EventSelectionScreen
import com.example.loglife.ui.logging.LogEventList
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.viewmodel.MainViewModel
import com.example.loglife.viewmodel.NavigationViewModel
import com.example.loglife.navigation.LogActivityDestination

import androidx.compose.runtime.*
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.home.HomeScreen

import com.example.loglife.navigation.HomeDestination
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Screen for logging a new activity.
 * Uses the actual EventSelectionScreen implementation.
 */
@Composable
fun LogActivityScreen(
    mainViewModel: MainViewModel = hiltViewModel(),
    navViewModel: NavigationViewModel = hiltViewModel()
) {
    val activeEvents by mainViewModel.activeEvents.collectAsStateWithLifecycle()
    EventSelectionScreen(
        viewModel = mainViewModel,
        onEventSelected = { eventTypeId: Long ->
            mainViewModel.logEvent(eventTypeId)
            navViewModel.navigateTo(HomeDestination)
        },
        onNavigateBack = { navViewModel.navigateTo(HomeDestination) },
        activeEvents = activeEvents,
        onStopEvent = { instanceId -> mainViewModel.stopDurationEvent(instanceId) }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun LogActivityScreenPreview() {
    // Never call LogActivityScreen() here: it resolves MainViewModel via hiltViewModel(),
    // which crashes the preview. Render the stateless list with sample data instead.
    val sampleTypes = listOf(
        EventType(id = 1, name = "Coffee", icon = "add", eventMode = EventMode.INSTANT, category = "Food"),
        EventType(id = 2, name = "Work", icon = "add", eventMode = EventMode.DURATION, category = "Work")
    )
    LoglifeTheme {
        LogEventList(
            eventTypes = sampleTypes,
            onEventClick = {}
        )
    }
}