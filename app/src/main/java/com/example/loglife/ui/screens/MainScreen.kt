package com.example.loglife.ui.screens

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
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.components.smartsidebar.SmartSidebar
import com.example.loglife.ui.home.HomeScreen
import com.example.loglife.ui.home.HomeScreenContent
import com.example.loglife.navigation.*
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.navigation.CalendarDestination
import com.example.loglife.navigation.LogActivityDestination
import com.example.loglife.viewmodel.NavigationViewModel
import com.example.loglife.navigation.StatsDestination

import com.example.loglife.navigation.HomeDestination

import com.example.loglife.ui.components.smartsidebar.SidebarDestination
import androidx.compose.ui.tooling.preview.Preview

/**
 * Main screen of the application that hosts the content and the smart sidebar.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    navViewModel: NavigationViewModel = viewModel()
) {
    // Collect navigation items and current destination from ViewModel
    val navigationItems = navViewModel.navigationItems
    val currentDestination by navViewModel.currentDestination.collectAsState()

    MainScreenShell(
        navigationItems = navigationItems,
        currentDestination = currentDestination,
        onDestinationSelected = { destination ->
            navViewModel.navigateTo(destination)
        },
        content = { destination ->
            when (destination) {
                is HomeDestination -> HomeScreen(
                    onNavigateToLogEvent = { navViewModel.navigateTo(LogActivityDestination) },
                    onNavigateToCalendar = { navViewModel.navigateTo(CalendarDestination) },
                    onNavigateToReport = { navViewModel.navigateTo(StatsDestination) },
                    onNavigateToEventManagement = { /* TODO */ },
                    onNavigateToSettings = { /* TODO */ }
                )
                is LogActivityDestination -> LogActivityScreen()
                is StatsDestination -> StatsScreen()
                is CalendarDestination -> CalendarScreen()
                else -> LogActivityScreen()
            }
        }
    )
}

/**
 * ViewModel-free shell: sidebar + scaffold + crossfade.
 * Previews use this directly with static content so no ViewModel
 * (Hilt or otherwise) is ever created inside a preview composition.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreenShell(
    navigationItems: List<NavigationItem>,
    currentDestination: Destination,
    onDestinationSelected: (Destination) -> Unit,
    content: @Composable (Destination) -> Unit
) {
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
        onDestinationSelected = onDestinationSelected,
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
                        content(destination)
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenPreview() {
    // Plain ViewModel with no dependencies: safe to construct directly in preview.
    // We deliberately do NOT call MainScreen() here: it composes HomeScreen(),
    // which resolves MainViewModel via hiltViewModel() and crashes the preview
    // (surfacing as a CompositionDataTree NPE in the tooling).
    val navigationItems = remember { NavigationViewModel().navigationItems }
    val now = DateTimeUtils.nowUtc()
    val sampleActive = listOf(
        EventInstanceWithType(
            eventInstance = EventInstance(id = 1, eventTypeId = 1, startTime = now - 45 * 60 * 1000),
            eventType = EventType(id = 1, name = "Work", icon = "work", eventMode = EventMode.DURATION, category = "Work")
        )
    )
    LoglifeTheme {
        MainScreenShell(
            navigationItems = navigationItems,
            currentDestination = HomeDestination,
            onDestinationSelected = {},
            content = {
                HomeScreenContent(
                    activeEvents = sampleActive,
                    recentEvent = null,
                    onLogEventClick = {},
                    onStopEventClick = {}
                )
            }
        )
    }
}
