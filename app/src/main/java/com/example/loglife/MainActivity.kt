package com.example.loglife

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CalendarToday
import dagger.hilt.android.AndroidEntryPoint
import com.example.loglife.ui.components.smartsidebar.SmartSidebar
import com.example.loglife.ui.components.smartsidebar.SidebarDestination
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.navigation.LifeLoggingNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoglifeTheme {
                LifeLoggingAppWithSmartSidebar()
            }
        }
    }
}

@Composable
fun LifeLoggingAppWithSmartSidebar() {
    val navController = rememberNavController()

    // Example: Select three existing destinations for quick access
    // Replace these with whatever three destinations make sense for your app's use case
    val quickAccessDestinations = listOf(
        // Example: Home screen
        SidebarDestination(
            destination = "home",
            label = "Home",
            icon = Icons.Default.Home
        ),
        // Example: Calendar screen
        SidebarDestination(
            destination = "calendar",
            label = "Calendar",
            icon = Icons.Default.CalendarToday
        ),
        // Example: Settings screen
        SidebarDestination(
            destination = "settings",
            label = "Settings",
            icon = Icons.Default.Settings
        )
    )

    // Get current destination from navigation controller
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Existing application navigation (unchanged)
        LifeLoggingNavGraph(
            modifier = Modifier.fillMaxSize()
        )

        // Smart Sidebar overlay - renders above existing navigation
        SmartSidebar(
            destinations = quickAccessDestinations,
            currentDestination = currentDestination,
            onDestinationSelected = { destination ->
                // Navigate using existing navigation controller
                navController.navigate(destination) {
                    // Optional: Customize navigation behavior
                    // For example, to avoid duplicates:
                    // launchSingleTop = true
                    // Or to pop up to a certain level:
                    // popUpTo(navController.graph.startDestinationId) { saveState = true }
                }

                // The sidebar will automatically close after navigation
                // due to the state change in onDestinationSelected
            }
        )
    }
}