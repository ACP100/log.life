package com.example.loglife.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.loglife.R
import com.example.loglife.ui.calendar.CalendarScreen
import com.example.loglife.ui.dayview.DayViewScreen
import com.example.loglife.ui.eventmanagement.EventManagementScreen
import com.example.loglife.ui.home.HomeScreen
import com.example.loglife.ui.logging.LogEventScreen
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.report.MonthlyReportScreen
import com.example.loglife.ui.settings.SettingsScreen
import com.example.loglife.ui.widgetsslot.WidgetSlotState
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Navigation graph for the Life Logging app.
 * Defines all destinations and navigation actions.
 */
@Composable
fun LifeLoggingNavGraph(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier.fillMaxSize()
    ) {
        // Home screen
        composable(
            route = "home"
        ) {
            HomeScreen(
                viewModel = mainViewModel,
                onNavigateToLogEvent = { navController.navigate("log_event") },
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToReport = { navController.navigate("monthly_report") },
                onNavigateToEventManagement = { navController.navigate("event_management") },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        // Log event screen
        composable(
            route = "log_event"
        ) {
            LogEventScreen(
                viewModel = mainViewModel,
                onEventSelected = { eventTypeId ->
                    mainViewModel.logEvent(eventTypeId)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Calendar screen
        composable(
            route = "calendar"
        ) {
            CalendarScreen(
                onDateSelected = { timestamp ->
                    navController.navigate("day_view_$timestamp") {
                        // Pop up to home screen before navigating to day view
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Day view screen
        composable(
            route = "day_view_{timestamp}",
            arguments = listOf(navArgument("timestamp") { type = NavType.LongType })
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
            DayViewScreen(
                selectedDateTimestamp = timestamp,
                onDateChanged = { newTimestamp ->
                    navController.navigate("day_view_$newTimestamp") {
                        // Pop up to the day view level and replace
                        popUpTo("day_view_*{timestamp}") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Monthly report screen
        composable(
            route = "monthly_report"
        ) {
            MonthlyReportScreen(
                selectedMonthTimestamp = System.currentTimeMillis(),
                onMonthChanged = { newMonthTimestamp ->
                    navController.navigate("monthly_report_$newMonthTimestamp") {
                        popUpTo("monthly_report") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Monthly report with specific month
        composable(
            route = "monthly_report_{timestamp}",
            arguments = listOf(navArgument("timestamp") { type = NavType.LongType })
        ) { backStackEntry ->
            val timestamp = backStackEntry.arguments?.getLong("timestamp") ?: 0L
            MonthlyReportScreen(
                selectedMonthTimestamp = timestamp,
                onMonthChanged = { newMonthTimestamp ->
                    navController.navigate("monthly_report_$newMonthTimestamp") {
                        popUpTo("monthly_report") { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Event management screen
        composable(
            route = "event_management"
        ) {
            EventManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable(
            route = "settings"
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}