package com.example.loglife.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.R
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.ui.home.ExpandableFloatingMenu

/**
 * Home screen showing current/last activity and quick log button.
 * This is the primary screen users see when opening the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToLogEvent: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToEventManagement: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEvents by viewModel.activeEvents.collectAsStateWithLifecycle()
    val recentEvent by viewModel.recentEvent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Life Log")
                },
                actions = { }
            )
        },
        content = { innerPadding ->

            if (isLoading) {
                CenteredLoadingIndicator()
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Event display (information only, no action buttons)
                    HomeScreenContent(
                        activeEvents = activeEvents,
                        recentEvent = recentEvent,
                        onLogEventClick = { /* Not used in UI - only for callbacks */ },
                        onStopEventClick = { /* Not used in UI - only for callbacks */ },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    )

                    // Expandable floating menu (replaces the + Log Event button)
                    ExpandableFloatingMenu(
                        onNavigateToCalendar = onNavigateToCalendar,
                        onNavigateToStatistics = onNavigateToReport,
                        onLogEvent = {
                            onNavigateToLogEvent()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    )
}

// Rest of the file remains unchanged...
@Composable
fun HomeScreenContent(
    activeEvents: List<EventInstanceWithType>,
    recentEvent: EventInstanceWithType?,
    onLogEventClick: (Long) -> Unit, // Kept for callback compatibility but not used in UI
    onStopEventClick: (Long) -> Unit, // Kept for callback compatibility but not used in UI
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        // Active Events Section (info only)
        if (activeEvents.isNotEmpty()) {
            ActiveEventsSection(
                activeEvents = activeEvents,
                onStopEventClick = { /* Not used in UI - only for callbacks */ }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Event Section (info only)
        recentEvent?.let { event ->
            RecentEventSection(
                event = event,
                onLogEventClick = { /* Not used in UI - only for callbacks */ }
            )
        }

        // If no events at all
        if (activeEvents.isEmpty() && recentEvent == null) {
            EmptyState()
        }
    }
}

@Composable
fun ActiveEventsSection(
    activeEvents: List<EventInstanceWithType>,
    onStopEventClick: (Long) -> Unit // Kept for callback compatibility but not used in UI
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "Currently",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeEvents) { event ->
                ActiveEventItem(
                    event = event,
                    onStopClick = { /* Not used in UI - only for callbacks */ }
                )
            }
        }
    }
}

@Composable
fun ActiveEventItem(
    event: EventInstanceWithType,
    onStopClick: () -> Unit // Kept for callback compatibility but not used in UI
) {
    val eventType = event.eventType
    val eventInstance = event.eventInstance

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
        // Icon
        Icon(
            imageVector = getIconResource(eventType.icon),
            contentDescription = eventType.name,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Event info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = eventType.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (eventType.eventMode == com.example.loglife.data.model.EventMode.DURATION) {
                if (eventInstance.endTime == null) {
                    // Active duration
                    Text(
                        text = "${DateTimeUtils.formatDuration(
                            eventInstance.startTime,
                            com.example.loglife.util.DateTimeUtils.nowUtc()
                        )} ●",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    // Completed duration
                    Text(
                        text = "${DateTimeUtils.formatTime(eventInstance.startTime)} → " +
                                "${DateTimeUtils.formatTime(eventInstance.endTime!!)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Instant event
                Text(
                    text = DateTimeUtils.formatTime(eventInstance.startTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecentEventSection(
    event: EventInstanceWithType,
    onLogEventClick: (Long) -> Unit // Kept for callback compatibility but not used in UI
) {
    val eventType = event.eventType
    val eventInstance = event.eventInstance

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        // Event name and time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = getIconResource(eventType.icon),
                contentDescription = eventType.name,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Event details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = eventType.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (eventType.eventMode) {
                        com.example.loglife.data.model.EventMode.INSTANT ->
                            DateTimeUtils.formatTime(eventInstance.startTime)
                        com.example.loglife.data.model.EventMode.DURATION ->
                            if (eventInstance.endTime == null) {
                                "${DateTimeUtils.formatDuration(
                                    eventInstance.startTime,
                                    com.example.loglife.util.DateTimeUtils.nowUtc()
                                )} ●"
                            } else {
                                "${DateTimeUtils.formatTime(eventInstance.startTime)} → " +
                                        "${DateTimeUtils.formatTime(eventInstance.endTime!!)}"
                            }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Timeline,
            contentDescription = "No events logged",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events logged yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to log your first event",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
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
    return Icons.Default.Event
}

