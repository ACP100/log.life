package com.example.loglife.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.loglife.R
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.widgetsslot.WidgetSlot
import com.example.loglife.ui.widgetsslot.WidgetSlotState
import com.example.loglife.util.DateTimeUtils
import android.util.Log
import android.widget.Toast

/**
 * Settings screen for configuring application behavior.
 * Includes reminder preferences, general settings, and app information.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)

            SettingsContent(
                onReminderIntervalChange = { hours ->
                    Log.d("SettingsScreen", "Reminder interval changed to: $hours hours")
                    // TODO: Implement actual reminder interval storage
                },
                onMaxDurationChange = { hours ->
                    Log.d("SettingsScreen", "Max duration changed to: $hours hours")
                    // TODO: Implement actual max duration storage
                },
                onWidgetConfigClick = {
                    Log.d("SettingsScreen", "Widget config clicked")
                    // TODO: Implement actual widget config screen
                },
                onAboutClick = {
                    Log.d("SettingsScreen", "About clicked")
                    // TODO: Implement actual about screen/dialog
                },
                modifier = contentModifier
            )
        }
    )
}

@Composable
fun SettingsContent(
    onReminderIntervalChange: (Int) -> Unit, // hours
    onMaxDurationChange: (Int) -> Unit, // hours
    onWidgetConfigClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Reminder Settings
        SettingsSection(
            title = "Reminders",
            icon = "notifications"
        ) {
            ReminderIntervalSetting(
                currentInterval = 4, // Default 4 hours
                onIntervalChange = onReminderIntervalChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            MaxDurationSetting(
                currentMaxDuration = 0, // 0 means no limit
                onMaxDurationChange = onMaxDurationChange
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Widget Settings
        SettingsSection(
            title = "Widget",
            icon = "widgets"
        ) {
            Button(
                onClick = onWidgetConfigClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = "Widget configuration",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Configure Widgets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Set up home screen widgets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // About Section
        SettingsSection(
            title = "About",
            icon = "info"
        ) {
            Button(
                onClick = onAboutClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "About Life Log",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getIconResource(icon),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun ReminderIntervalSetting(
    currentInterval: Int,
    onIntervalChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Duration Event Reminders",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Get notified when a duration event has been running for a set time",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Interval options
        val intervals = listOf(
            Pair(0, "Never"),
            Pair(2, "2 hours"),
            Pair(4, "4 hours"),
            Pair(6, "6 hours"),
            Pair(8, "8 hours"),
            Pair(12, "12 hours"),
            Pair(24, "24 hours")
        )

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            RadioButton(
                selected = currentInterval == 0,
                onClick = { onIntervalChange(0) },
                enabled = true
            )
            Text(
                text = "Never",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onIntervalChange(0) }
            )
        }

        intervals.filter { it.first != 0 }.forEach { (hours, label) ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = currentInterval == hours,
                    onClick = { onIntervalChange(hours) },
                    enabled = true
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onIntervalChange(hours) }
                )
            }
        }
    }
}

@Composable
fun MaxDurationSetting(
    currentMaxDuration: Int,
    onMaxDurationChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Maximum Duration",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Automatically stop duration events after this time (0 = no limit)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Max duration options
        val durations = listOf(
            Pair(0, "No limit"),
            Pair(1, "1 hour"),
            Pair(2, "2 hours"),
            Pair(4, "4 hours"),
            Pair(6, "6 hours"),
            Pair(8, "8 hours"),
            Pair(12, "12 hours"),
            Pair(24, "24 hours")
        )

        durations.forEach { (hours, label) ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = currentMaxDuration == hours,
                    onClick = { onMaxDurationChange(hours) },
                    enabled = true
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { onMaxDurationChange(hours) }
                )
            }
        }
    }
}

@Composable
fun CenteredLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun getIconResource(iconName: String): ImageVector {
    return Icons.Default.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                title()
            }
        },
        actions = actions,
        modifier = modifier
    )
}