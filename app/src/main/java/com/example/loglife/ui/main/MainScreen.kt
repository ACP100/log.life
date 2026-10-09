package com.example.loglife.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.loglife.ui.components.smartsidebar.SmartSidebar
import com.example.loglife.ui.navigation.*
import com.example.loglife.viewmodel.CalendarDestination
import com.example.loglife.viewmodel.LogActivityDestination
import com.example.loglife.viewmodel.NavigationViewModel
import com.example.loglife.viewmodel.StatsDestination
import com.example.loglife.ui.screens.CalendarScreen
import com.example.loglife.ui.screens.LogActivityScreen
import com.example.loglife.ui.screens.StatsScreen

import com.example.loglife.viewmodel.HomeDestination
import com.example.loglife.ui.home.HomeScreen

import com.example.loglife.ui.components.smartsidebar.SidebarDestination

/**
 * Main screen of the application that hosts the content and the smart sidebar.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val viewModel: NavigationViewModel = viewModel()

    // Collect navigation items and current destination from ViewModel
    val navigationItems = viewModel.navigationItems
    val currentDestination by viewModel.currentDestination.collectAsState()

    // Map navigation items to sidebar destinations
    val sidebarDestinations = navigationItems.map { item ->
        SidebarDestination(
            destination = item.destination,
            label = item.label,
            icon = item.icon
        )
    }

    SmartSidebar(
        destinations = sidebarDestinations,
        currentDestination = currentDestination,
        onDestinationSelected = { destination ->
            viewModel.navigateTo(destination)
        },
        content = {
            Scaffold() { padding ->
                // Main content area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Crossfade between screens for seamless transitions
                    Crossfade(
                        targetState = currentDestination,
                        animationSpec = tween(durationMillis = 300)
                    ) { destination ->
                        when (destination) {
                            is HomeDestination -> HomeScreen(
                                onNavigateToLogEvent = { viewModel.navigateTo(LogActivityDestination) },
                                onNavigateToCalendar = { viewModel.navigateTo(CalendarDestination) },
                                onNavigateToReport = { viewModel.navigateTo(StatsDestination) },
                                onNavigateToEventManagement = { /* TODO */ },
                                onNavigateToSettings = { /* TODO */ }
                            )
                            is LogActivityDestination -> LogActivityScreen()
                            is StatsDestination -> StatsScreen()
                            is CalendarDestination -> CalendarScreen()
                            else -> LogActivityScreen()
                        }
                    }
                }
            }
        }
    )
}


