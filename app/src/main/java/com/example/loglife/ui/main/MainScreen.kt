package com.example.loglife.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.navigation.ButtonsWidgetDestination
import com.example.loglife.navigation.DeckWidgetDestination
import com.example.loglife.navigation.DeleteDataDestination
import com.example.loglife.navigation.Destination
import com.example.loglife.navigation.EventsDestination
import com.example.loglife.navigation.EventDetailDestination
import com.example.loglife.navigation.HistoryDestination
import com.example.loglife.navigation.HomeDestination
import com.example.loglife.navigation.NavigationItem
import com.example.loglife.navigation.NavigationViewModel
import com.example.loglife.navigation.SettingsDestination
import com.example.loglife.navigation.TileWidgetDestination
import com.example.loglife.navigation.TodayWidgetDestination
import com.example.loglife.navigation.WidgetFavouritesDestination
import com.example.loglife.navigation.WidgetManagerDestination
import com.example.loglife.ui.components.smartsidebar.SidebarDestination
import com.example.loglife.ui.components.smartsidebar.SmartSidebar
import com.example.loglife.ui.history.EventDetailScreen
import com.example.loglife.ui.history.HistoryPeriod
import com.example.loglife.ui.history.HistoryScreen
import com.example.loglife.ui.history.HistoryViewModel
import com.example.loglife.ui.home.HomeScreen
import com.example.loglife.ui.home.HomeScreenContent
import com.example.loglife.ui.home.buildHomeUiState
import com.example.loglife.ui.settings.DeleteDataScreen
import com.example.loglife.ui.settings.EventsScreen
import com.example.loglife.ui.settings.SettingsScreen
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.widgetmanager.WidgetDetailScreen
import com.example.loglife.ui.widgetmanager.WidgetFavouritesScreen
import com.example.loglife.ui.widgetmanager.WidgetManagerScreen
import com.example.loglife.ui.widgets.WidgetKind
import com.example.loglife.util.DateTimeUtils
import java.time.LocalDate

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
    val backStack by navViewModel.backStack.collectAsState()
    val logSheetRequested by navViewModel.logSheetRequested.collectAsState()
    val fixEndRequest by navViewModel.fixEndRequest.collectAsState()

    // System back pops the nav stack; at the root (Home) it falls through
    // and the system handles it (exits the app).
    BackHandler(enabled = backStack.size > 1) {
        navViewModel.navigateBack()
    }

    val historyViewModel: HistoryViewModel = hiltViewModel()

    MainScreenShell(
        navigationItems = navigationItems,
        currentDestination = currentDestination,
        onDestinationSelected = { destination ->
            navViewModel.navigateTo(destination)
        },
        content = { destination ->
            when (destination) {
                is HomeDestination -> HomeScreen(
                    onSeeYesterday = {
                        historyViewModel.show(HistoryPeriod.DAY, LocalDate.now().minusDays(1))
                        navViewModel.navigateTo(HistoryDestination)
                    },
                    logSheetRequested = logSheetRequested,
                    onLogSheetRequestHandled = navViewModel::onLogSheetRequestHandled,
                    fixEndInstanceId = fixEndRequest,
                    onFixEndHandled = navViewModel::onFixEndHandled
                )
                is HistoryDestination -> HistoryScreen(
                    onOpenEvent = { navViewModel.navigateTo(EventDetailDestination(it)) },
                    viewModel = historyViewModel
                )
                is EventDetailDestination -> EventDetailScreen(
                    eventTypeId = destination.eventTypeId,
                    onBack = { navViewModel.navigateBack() },
                    onOpenDay = { date ->
                        historyViewModel.show(HistoryPeriod.DAY, date)
                        navViewModel.navigateBack()
                    }
                )
                is SettingsDestination -> SettingsScreen(
                    onOpenEvents = { navViewModel.navigateTo(EventsDestination) },
                    onOpenWidgets = { navViewModel.navigateTo(WidgetManagerDestination) },
                    onOpenDeleteData = { navViewModel.navigateTo(DeleteDataDestination) }
                )
                is EventsDestination -> EventsScreen(onBack = { navViewModel.navigateBack() })
                is DeleteDataDestination -> DeleteDataScreen(
                    onBack = { navViewModel.navigateBack() },
                    onDeleted = { navViewModel.navigateTo(HomeDestination) }
                )
                is WidgetManagerDestination -> WidgetManagerScreen(
                    onBack = { navViewModel.navigateBack() },
                    onOpenWidget = { kind ->
                        navViewModel.navigateTo(
                            when (kind) {
                                WidgetKind.DECK -> DeckWidgetDestination
                                WidgetKind.BUTTONS -> ButtonsWidgetDestination
                                WidgetKind.TILE -> TileWidgetDestination
                                WidgetKind.TODAY -> TodayWidgetDestination
                            }
                        )
                    }
                )
                is DeckWidgetDestination, is ButtonsWidgetDestination, is TileWidgetDestination, is TodayWidgetDestination -> WidgetDetailScreen(
                    kind = when (destination) {
                        is DeckWidgetDestination -> WidgetKind.DECK
                        is ButtonsWidgetDestination -> WidgetKind.BUTTONS
                        is TileWidgetDestination -> WidgetKind.TILE
                        else -> WidgetKind.TODAY
                    },
                    onBack = { navViewModel.navigateBack() },
                    onOpenFavourites = { navViewModel.navigateTo(WidgetFavouritesDestination) }
                )
                is WidgetFavouritesDestination -> WidgetFavouritesScreen(onBack = { navViewModel.navigateBack() })
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
                    state = buildHomeUiState(today = sampleActive, active = sampleActive),
                    date = LocalDate.now(),
                    now = now,
                    onStop = {},
                    onLogSomething = {},
                    onSeeYesterday = {}
                )
            }
        )
    }
}
