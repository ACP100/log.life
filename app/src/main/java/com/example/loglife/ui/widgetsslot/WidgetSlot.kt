package com.example.loglife.ui.widgetsslot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.loglife.R
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventType
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils

/**
 * A slot/widget that can display different content based on its state.
 * Used in the widget implementation for switching between different views.
 */
@Composable
fun WidgetSlot(
    state: WidgetSlotState,
    modifier: Modifier = Modifier,
    content: @Composable (WidgetSlotState) -> Unit,
    onRetry: (() -> Unit)? = null
) {
    when (state) {
        is WidgetSlotState.Loading -> {
            WidgetSlotLoading()
        }
        is WidgetSlotState.Content -> {
            content(state)
        }
        is WidgetSlotState.Empty -> {
            WidgetSlotEmpty()
        }
        is WidgetSlotState.Error -> {
            WidgetSlotError(state.errorMessage, onRetry)
        }
    }
}

@Composable
fun WidgetSlotLoading() {
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

@Composable
fun WidgetSlotEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventAvailable,
            contentDescription = "No content",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No content available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WidgetSlotError(errorMessage: String, onRetry: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { onRetry?.invoke() },
            enabled = onRetry != null,
            modifier = Modifier.width(120.dp)
        ) {
            Text("Retry")
        }
    }
}

/**
 * State classes for WidgetSlot
 */
sealed class WidgetSlotState {
    object Loading : WidgetSlotState()
    data class Content(val data: Any) : WidgetSlotState()
    object Empty : WidgetSlotState()
    data class Error(val errorMessage: String) : WidgetSlotState()
}

/**
 * Specific slot implementations for different widget modes
 */
@Composable
fun CurrentLogSlot(
    activeEvents: List<EventInstanceWithType>,
    onStopEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeEvents.isEmpty()) {
        WidgetSlotEmpty()
        return
    }

    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Currently",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(activeEvents) { event ->
                CurrentLogEventItem(
                    event = event,
                    onStopClick = { onStopEventClick(event.eventInstance.id) }
                )
            }
        }
    }
}

@Composable
fun CurrentLogEventItem(
    event: EventInstanceWithType,
    onStopClick: () -> Unit
) {
    val eventType = event.eventType
    val eventInstance = event.eventInstance

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
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
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Event info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = eventType.name,
                style = MaterialTheme.typography.bodyLarge,
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

        // Stop button (only for active duration events)
        if (eventType.eventMode == com.example.loglife.data.model.EventMode.DURATION &&
            eventInstance.endTime == null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onStopClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddLogSlot(
    widgetEvents: List<EventType>,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (widgetEvents.isEmpty()) {
        WidgetSlotEmpty()
        return
    }

    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        Text(
            text = "Quick Log",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(widgetEvents) { eventType ->
                AddLogEventItem(
                    eventType = eventType,
                    onClick = { onEventClick(eventType.id) }
                )
            }
        }
    }
}

@Composable
fun AddLogEventItem(
    eventType: EventType,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
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
        }
    }
}

/**
 * Both slot - combines current log and add log in a swipeable interface
 * For simplicity in this implementation, we'll show both sections vertically
 */
@Composable
fun BothSlot(
    activeEvents: List<EventInstanceWithType>,
    widgetEvents: List<EventType>,
    onStopEventClick: (Long) -> Unit,
    onEventClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Current Log Section
        if (activeEvents.isNotEmpty()) {
            CurrentLogSlot(
                activeEvents = activeEvents,
                onStopEventClick = onStopEventClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        } else {
            WidgetSlotEmpty(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        // Add Log Section
        AddLogSlot(
            widgetEvents = widgetEvents,
            onEventClick = onEventClick,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

private fun getIconResource(iconName: String): ImageVector {
    return Icons.Default.Event
}

// Sample data shared by the previews below (no ViewModel required).
private val previewActiveEvents = listOf(
    EventInstanceWithType(
        eventInstance = EventInstance(id = 1, eventTypeId = 1, startTime = DateTimeUtils.nowUtc() - 45 * 60 * 1000),
        eventType = EventType(id = 1, name = "Work", icon = "work", eventMode = EventMode.DURATION, category = "Work")
    )
)
private val previewWidgetEvents = listOf(
    EventType(id = 1, name = "Coffee", icon = "coffee", eventMode = EventMode.INSTANT, category = "Food"),
    EventType(id = 2, name = "Work", icon = "work", eventMode = EventMode.DURATION, category = "Work")
)

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun WidgetSlotLoadingPreview() {
    LoglifeTheme { WidgetSlotLoading() }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 200)
@Composable
fun WidgetSlotEmptyPreview() {
    LoglifeTheme { WidgetSlotEmpty() }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 280)
@Composable
fun WidgetSlotErrorPreview() {
    LoglifeTheme {
        WidgetSlotError(errorMessage = "Couldn't load events", onRetry = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 280)
@Composable
fun WidgetSlotErrorNoRetryPreview() {
    LoglifeTheme {
        WidgetSlotError(errorMessage = "Couldn't load events", onRetry = null)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
fun CurrentLogSlotPreview() {
    LoglifeTheme {
        CurrentLogSlot(activeEvents = previewActiveEvents, onStopEventClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
fun AddLogSlotPreview() {
    LoglifeTheme {
        AddLogSlot(widgetEvents = previewWidgetEvents, onEventClick = {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun BothSlotPreview() {
    LoglifeTheme {
        BothSlot(
            activeEvents = previewActiveEvents,
            widgetEvents = previewWidgetEvents,
            onStopEventClick = {},
            onEventClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 400)
@Composable
fun WidgetSlotContentStatePreview() {
    LoglifeTheme {
        WidgetSlot(
            state = WidgetSlotState.Content("preview"),
            onRetry = {},
            content = {
                CurrentLogSlot(activeEvents = previewActiveEvents, onStopEventClick = {})
            }
        )
    }
}
