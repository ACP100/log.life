package com.example.loglife.ui.logging

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.R
import com.example.loglife.data.model.EventType
import com.example.loglife.viewmodel.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.loglife.data.model.EventMode
import java.util.*

/**
 * Screen for selecting and logging an event type.
 * Shows a list of available event types that can be logged with one tap.
 * This component is typically used within a larger screen layout for activity logging.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSelectionScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onEventSelected: (Long) -> Unit, // eventTypeId
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventTypes by viewModel.eventTypes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Log Event") },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

            if (isLoading) {
                CenteredLoadingIndicator()
            } else if (eventTypes.isEmpty()) {
                EmptyEventTypesState(onNavigateBack = onNavigateBack)
            } else {
                LogEventList(
                    eventTypes = eventTypes,
                    onEventClick = { eventType ->
                        // For instant events, log immediately
                        // For duration events, start the event
                        onEventSelected(eventType.id)
                        onNavigateBack() // Return to home screen
                    },
                    modifier = contentModifier
                )
            }
        }
    )
}

@Composable
fun LogEventList(
    eventTypes: List<EventType>,
    onEventClick: (EventType) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group event types by category for better organization
    val groupedEvents = eventTypes.groupBy { it.category ?: "Uncategorized" }

    val sections = groupedEntriesToList(groupedEvents)
    LazyColumn(
        modifier = modifier
    ) {
        sections.forEachIndexed { index, groupedItem ->
            val (category, eventsInCategory) = groupedItem

            // Category header
            if (eventsInCategory.isNotEmpty()) {
                item {
                    CategoryHeader(
                        category = category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }

            // Events in this category
            items(eventsInCategory.size) { eventIndex ->
                val eventType = eventsInCategory[eventIndex]
                EventTypeItem(
                    eventType = eventType,
                    onClick = { onEventClick(eventType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = if (eventIndex == 0 && index > 0) 8.dp else 4.dp,
                            horizontal = 16.dp
                        )
                )
            }

            // Divider between categories (except after last category)
            if (index < sections.lastIndex) {
                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    category: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Text(
            text = category.capitalize(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
fun EventTypeItem(
    eventType: EventType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWidgetVisible = eventType.showInWidget

    Card(
        modifier = modifier
            .clickable { onClick() }
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (eventType.category != null) {
                    Text(
                        text = eventType.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Widget visibility indicator
            if (isWidgetVisible) {
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Show in widget",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyEventTypesState(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventAvailable,
            contentDescription = "No event types available",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No event types available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create event types in Event Management",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Navigate to Event Management */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Manage Event Types")
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

private fun groupedEntriesToList(groupedEvents: Map<String, List<EventType>>): List<Pair<String, List<EventType>>> {
    return groupedEvents.toList()
}

private fun getIconResource(iconName: String): ImageVector {
    return Icons.Default.Add
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

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun EventSelectionScreenPreview() {
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
