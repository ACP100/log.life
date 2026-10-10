package com.example.loglife.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.loglife.navigation.*
import com.example.loglife.ui.theme.LoglifeTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Widgets
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.loglife.ui.widgets.EventLogWidgetProvider
import com.example.loglife.viewmodel.NavigationViewModel

/**
 * Settings screen that lists all available screens in the app.
 */
@Composable
fun SettingsScreen(
    navViewModel: NavigationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    // No local BackHandler: with no internal state to clear, system back falls
    // through to MainScreen's handler, which pops the nav stack.
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // List of all available screens
        Column(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(16.dp)
        ) {
            SettingsScreenItem(
                label = "Home",
                icon = Icons.Default.Home,
                onClick = { navViewModel.navigateTo(HomeDestination) }
            )
            Spacer(Modifier.height(12.dp))
            SettingsScreenItem(
                label = "Log Activity",
                icon = Icons.Default.AccessTime,
                onClick = { navViewModel.navigateTo(LogActivityDestination) }
            )
            Spacer(Modifier.height(12.dp))
            SettingsScreenItem(
                label = "View Stats",
                icon = Icons.Default.ShowChart,
                onClick = { navViewModel.navigateTo(StatsDestination) }
            )
            Spacer(Modifier.height(12.dp))
            SettingsScreenItem(
                label = "Calendar",
                icon = Icons.Default.CalendarToday,
                onClick = { navViewModel.navigateTo(CalendarDestination) }
            )
            Spacer(Modifier.height(12.dp))
            SettingsScreenItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                onClick = { navViewModel.navigateTo(SettingsDestination) }
            )
            Spacer(Modifier.height(12.dp))
            SettingsScreenItem(
                label = "Add widget",
                icon = Icons.Default.Widgets,
                onClick = {
                    // Ask the launcher to pin the widget; the system then runs
                    // EventLogWidgetConfigureActivity (android:configure) automatically.
                    // (minSdk 26, so requestPinAppWidget is always available.)
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                    val provider = ComponentName(context, EventLogWidgetProvider::class.java)
                    appWidgetManager?.requestPinAppWidget(provider, null, null)
                }
            )
        }
    }
}

/**
 * Individual settings item representing a navigable screen.
 */
@Composable
private fun SettingsScreenItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Go to $label",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun SettingsScreenPreview() {
    LoglifeTheme {
        SettingsScreen(
            onNavigateBack = {}
        )
    }
}