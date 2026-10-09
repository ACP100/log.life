package com.example.loglife.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.ui.logging.LogEventScreen
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.viewmodel.NavigationViewModel
import com.example.loglife.viewmodel.LogActivityDestination

import androidx.compose.runtime.*
import com.example.loglife.ui.home.HomeScreen

import com.example.loglife.viewmodel.HomeDestination

/**
 * Screen for logging a new activity.
 * Uses the actual LogEventScreen implementation.
 */
@Composable
fun LogActivityScreen() {
    val navViewModel: NavigationViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    LogEventScreen(
        viewModel = mainViewModel,
        onEventSelected = { eventTypeId: Long ->
            mainViewModel.logEvent(eventTypeId)
            navViewModel.navigateTo(HomeDestination)
        },
        onNavigateBack = { navViewModel.navigateTo(HomeDestination) }
    )
}