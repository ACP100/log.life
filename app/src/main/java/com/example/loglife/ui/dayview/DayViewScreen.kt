package com.example.loglife.ui.dayview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.loglife.R
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.ui.widgetsslot.WidgetSlot
import com.example.loglife.ui.widgetsslot.WidgetSlotState

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.model.EventMode
import com.example.loglife.ui.logging.LogEventScreen
import com.example.loglife.ui.logging.EventEditDialog
import com.example.loglife.ui.logging.NoteEditDialog

/**
 * Day view screen showing chronological timeline of events for a specific date.
 * Includes active events, event timeline, notes, editing capabilities, and unlogged time gaps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    viewModel: MainViewModel = hiltViewModel(),
    selectedDateTimestamp: Long, // Start of day in UTC milliseconds
    onDateChanged: (Long) -> Unit, // Callback for date navigation
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayEvents = rememberDayEventsState(selectedDateTimestamp, viewModel)
    val activeEvents = rememberActiveEventsState(viewModel)
    val isLoading = rememberIsLoadingState(viewModel)
    val currentTimeUtc = DateTimeUtils.nowUtc()

    // Collect ViewModel UI event flows
    var showLogEventDialog by remember { mutableStateOf(false) }
    var showEventEditDialog by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<EventInstanceWithType?>(null) }
    var showNoteEditDialog by remember { mutableStateOf(false) }
    var eventToEditNote by remember { mutableStateOf<EventInstanceWithType?>(null) }

    LaunchedEffect(viewModel) {
        // Collect navigate to log event flow
        viewModel.navigateToLogEvent.collect {
            showLogEventDialog = true
        }
    }

    LaunchedEffect(viewModel) {
        // Collect show event edit dialog flow
        viewModel.showEventEditDialog.collect { event ->
            eventToEdit = event
            showEventEditDialog = true
        }
    }

    LaunchedEffect(viewModel) {
        // Collect show note edit dialog flow
        viewModel.showNoteEditDialog.collect { event ->
            eventToEditNote = event
            showNoteEditDialog = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(formatDateHeader(selectedDateTimestamp)) },
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

            if (isLoading) {
                CenteredLoadingIndicator()
            } else {
                DayViewContent(
                    selectedDateTimestamp = selectedDateTimestamp,
                    dayEvents = dayEvents,
                    activeEvents = activeEvents,
                    currentTimeUtc = currentTimeUtc,
                    onDateChanged = onDateChanged,
                    onEventClick = { event ->
                        // TODO: Navigate to event editing - show edit dialog or navigate to edit screen
                        viewModel.startEventEditing(event)
                    },
                    onNoteClick = { event ->
                        // TODO: Open note editor - show note edit dialog
                        viewModel.openNoteEditor(event)
                    },
                    onLogEventClick = { eventTypeId ->
                        // TODO: Handle quick log - show quick log dialog for this event type
                        viewModel.logQuickEvent(eventTypeId)
                    },
                    modifier = contentModifier,
                    viewModel = viewModel
                )
            }
            // Dialogs (shown on top of content)
            if (showLogEventDialog) {
                LogEventScreen(
                    viewModel = viewModel,
                    onEventSelected = { eventTypeId ->
                        viewModel.logEvent(eventTypeId)
                        showLogEventDialog = false
                    },
                    onNavigateBack = { showLogEventDialog = false }
                )
            }

            if (showEventEditDialog) {
                eventToEdit?.let { event ->
                    EventEditDialog(
                        event = event,
                        onSaveClick = { updatedEvent ->
                            viewModel.updateEventInstance(updatedEvent)
                            showEventEditDialog = false
                            eventToEdit = null
                        },
                        onCancelClick = {
                            showEventEditDialog = false
                            eventToEdit = null
                        }
                    )
                }
            }

            if (showNoteEditDialog) {
                eventToEditNote?.let { event ->
                    NoteEditDialog(
                        event = event,
                        onSaveClick = { updatedNote ->
                            viewModel.updateEventNote(event.eventInstance.id, updatedNote)
                            showNoteEditDialog = false
                            eventToEditNote = null
                        },
                        onCancelClick = {
                            showNoteEditDialog = false
                            eventToEditNote = null
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun DayViewContent(
    selectedDateTimestamp: Long,
    dayEvents: List<EventInstanceWithType>,
    activeEvents: List<EventInstanceWithType>,
    currentTimeUtc: Long,
    onDateChanged: (Long) -> Unit,
    onEventClick: (EventInstanceWithType) -> Unit,
    onNoteClick: (EventInstanceWithType) -> Unit,
    onLogEventClick: (Long) -> Unit,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dayStart = selectedDateTimestamp
    val dayEnd = DateTimeUtils.getEndOfDay(selectedDateTimestamp)

    Column(
        modifier = modifier
    ) {
        // Active Events Section (if any)
        if (activeEvents.isNotEmpty()) {
            ActiveEventsBanner(
                activeEvents = activeEvents,
                currentTimeUtc = currentTimeUtc,
                viewModel = viewModel
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Timeline
        Timeline(
            dayEvents = dayEvents,
            dayStart = dayStart,
            dayEnd = dayEnd,
            currentTimeUtc = currentTimeUtc,
            onEventClick = onEventClick,
            onNoteClick = onNoteClick,
            viewModel = viewModel
        )

        // Quick log button at bottom
        Spacer(modifier = Modifier.height(24.dp))
        QuickLogButton(
            onClick = {
                // TODO: Show log event dialog - show dialog to select event type and log
                viewModel.showLogEventDialog()
            }
        )
    }
}

@Composable
fun ActiveEventsBanner(
    activeEvents: List<EventInstanceWithType>,
    currentTimeUtc: Long,
    viewModel: MainViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer
            )
    ) {
        // Active events icons and times
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            activeEvents.forEachIndexed { index, event ->
                if (index > 0) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                ActiveEventBadge(
                    event = event,
                    currentTimeUtc = currentTimeUtc
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // + Log Event button
        Button(
            onClick = {
                // TODO: Show log event dialog - show dialog to select event type and log
                viewModel.showLogEventDialog()
            },
            modifier = Modifier.height(36.dp)
        ) {
            Text("+ Log Event")
        }
    }
}

@Composable
fun ActiveEventBadge(
    event: EventInstanceWithType,
    currentTimeUtc: Long
) {
    val eventType = event.eventType
    val eventInstance = event.eventInstance

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = getIconResource(eventType.icon),
            contentDescription = eventType.name,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (eventType.eventMode == EventMode.DURATION) {
            if (eventInstance.endTime == null) {
                // Active duration
                Text(
                    text = DateTimeUtils.formatDuration(
                        eventInstance.startTime,
                        currentTimeUtc
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                // Completed duration - show end time
                Text(
                    text = DateTimeUtils.formatTime(eventInstance.endTime!!),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            // Instant event
            Text(
                text = DateTimeUtils.formatTime(eventInstance.startTime),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun Timeline(
    dayEvents: List<EventInstanceWithType>,
    dayStart: Long,
    dayEnd: Long,
    currentTimeUtc: Long,
    onEventClick: (EventInstanceWithType) -> Unit,
    onNoteClick: (EventInstanceWithType) -> Unit,
    viewModel: MainViewModel
) {
    if (dayEvents.isEmpty()) {
        EmptyTimelineState(
            dayStart = dayStart,
            dayEnd = dayEnd,
            onLogEventClick = { eventTypeId ->
                // TODO: Handle quick log - show quick log dialog for this event type
                viewModel.logQuickEvent(eventTypeId)
            }
        )
        return
    }

    // Sort events by start time
    val sortedEvents = dayEvents.sortedBy { it.eventInstance.startTime }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Add time header for the day
        item {
            TimeHeader(
                time = dayStart,
                label = "Start of day",
                isDayHeader = true
            )
        }

        // Events
        itemsIndexed(sortedEvents) { index, eventWithType ->
            val event = eventWithType.eventInstance
            val eventType = eventWithType.eventType

            // Time separator between events
            if (index > 0) {
                val previousEvent = sortedEvents[index - 1].eventInstance
                val previousEndTime = previousEvent.endTime ?: previousEvent.startTime
                val timeGap = event.startTime - previousEndTime
                if (timeGap > 5 * 60 * 1000) { // More than 5 minutes gap
                    TimeGapIndicator(
                        startTime = previousEndTime,
                        endTime = event.startTime
                    )
                }
            }

            EventTimelineItem(
                eventWithType = eventWithType,
                dayStart = dayStart,
                dayEnd = dayEnd,
                currentTimeUtc = currentTimeUtc,
                onClick = { onEventClick(eventWithType) },
                onNoteClick = { onNoteClick(eventWithType) },
                onDragResizeClick = { viewModel.startDragResizeMode(eventWithType) }
            )
        }

        // Add time header for end of day
        item {
            TimeHeader(
                time = dayEnd,
                label = "End of day",
                isDayHeader = true
            )
        }
    }
}

@Composable
fun EventTimelineItem(
    eventWithType: EventInstanceWithType,
    dayStart: Long,
    dayEnd: Long,
    currentTimeUtc: Long,
    onClick: () -> Unit,
    onNoteClick: () -> Unit,
    onDragResizeClick: () -> Unit
) {
    val eventInstance = eventWithType.eventInstance
    val eventType = eventWithType.eventType
    val isActive = eventType.eventMode == EventMode.DURATION &&
                   eventInstance.endTime == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .background(
                color = if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
    ) {
        // Time column
        Column(
            modifier = Modifier
                .width(64.dp)
                .padding(end = 16.dp)
        ) {
            TimeColumn(
                eventInstance = eventInstance,
                eventType = eventType.eventMode,
                dayStart = dayStart,
                dayEnd = dayEnd,
                currentTimeUtc = currentTimeUtc
            )
        }

        // Event content
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            // Event name and icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getIconResource(eventType.icon),
                    contentDescription = eventType.name,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Time range
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatEventTimeRange(
                        eventInstance = eventInstance,
                        eventType = eventType.eventMode,
                        currentTimeUtc = currentTimeUtc
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Note indicator
                if (eventInstance.note?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onNoteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = "View note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Drag handle for resizing (for duration events)
        if (eventType.eventMode == EventMode.DURATION) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Adjust timing",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
                    .clickable {
                        onDragResizeClick()
                    }
            )
        }
    }
}

@Composable
fun TimeColumn(
    eventInstance: EventInstance,
    eventType: EventMode,
    dayStart: Long,
    dayEnd: Long,
    currentTimeUtc: Long
) {
    val startTimeY = calculateTimePosition(
        timestamp = eventInstance.startTime,
        dayStart = dayStart,
        dayEnd = dayEnd
    )
    val endTimeY = if (eventInstance.endTime != null) {
        calculateTimePosition(
            timestamp = eventInstance.endTime!!,
            dayStart = dayStart,
            dayEnd = dayEnd
        )
    } else {
        // For active events, calculate current position
        calculateTimePosition(
            timestamp = currentTimeUtc,
            dayStart = dayStart,
            dayEnd = dayEnd
        )
    }

    Box(
        modifier = Modifier
            .width(32.dp)
            .height(64.dp)
    ) {
        // Time line
        DrawTimeLine(
            startY = startTimeY,
            endY = endTimeY,
            isActive = eventInstance.endTime == null &&
                    eventType == EventMode.DURATION
        )

        // Start time
        if (eventInstance.endTime == null ||
            !DateTimeUtils.isSameDay(eventInstance.startTime, eventInstance.endTime!!)) {
            Text(
                text = DateTimeUtils.formatTime(eventInstance.startTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 4.dp, y = startTimeY.dp - 12.dp)
            )
        }

        // End time (for completed events or if different day)
        if (eventInstance.endTime != null &&
            (!DateTimeUtils.isSameDay(eventInstance.startTime, eventInstance.endTime!!) ||
             eventInstance.endTime == null)) {
            Text(
                text = DateTimeUtils.formatTime(eventInstance.endTime!!),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .offset(x = 4.dp, y = endTimeY.dp + 4.dp)
            )
        }
    }
}

@Composable
fun DrawTimeLine(
    startY: Int,
    endY: Int,
    isActive: Boolean
) {
    val brush = if (isActive) {
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary
            )
        )
    } else {
        SolidColor(MaterialTheme.colorScheme.outline)
    }

    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(2.dp)) {
        drawLine(
            start = Offset(1f, startY.toFloat()),
            end = Offset(1f, endY.toFloat()),
            color = lineColor,
            strokeWidth = 2f
        )
    }
}

@Composable
fun TimeHeader(
    time: Long,
    label: String,
    isDayHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        if (isDayHeader) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
            )
        } else {
            Text(
                text = DateTimeUtils.formatTime(time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
            )
        }
    }
}

@Composable
fun TimeGapIndicator(
    startTime: Long,
    endTime: Long
) {
    val gapDuration = endTime - startTime
    val hours = gapDuration / (60 * 60 * 1000)
    val minutes = (gapDuration % (60 * 60 * 1000)) / (60 * 1000)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant
            )
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = "Time gap",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "No events logged",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${if (hours > 0) "$hours h " else ""}$minutes m",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyTimelineState(
    dayStart: Long,
    dayEnd: Long,
    onLogEventClick: (Long) -> Unit
) {
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
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events logged for ${DateTimeUtils.formatDate(dayStart)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to log an event",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun QuickLogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Log event",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Event",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
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

/**
 * Helper functions
 */
private fun formatEventTimeRange(
    eventInstance: EventInstance,
    eventType: EventMode,
    currentTimeUtc: Long
): String {
    return when (eventType) {
        EventMode.INSTANT ->
            DateTimeUtils.formatTime(eventInstance.startTime)
        EventMode.DURATION ->
            if (eventInstance.endTime == null) {
                // Active event
                "${DateTimeUtils.formatTime(eventInstance.startTime)} → " +
                        "${DateTimeUtils.formatDuration(
                            eventInstance.startTime,
                            currentTimeUtc
                        )} ●"
            } else {
                // Completed event
                "${DateTimeUtils.formatTime(eventInstance.startTime)} → " +
                        "${DateTimeUtils.formatTime(eventInstance.endTime!!)}"
            }
    }
}

private fun calculateTimePosition(timestamp: Long, dayStart: Long, dayEnd: Long): Int {
    val dayLength = dayEnd - dayStart
    val offsetFromStart = timestamp - dayStart
    val percentage = offsetFromStart.toFloat() / dayLength
    return (percentage * 100).toInt() // 0-100% of the container height
}

private fun getIconResource(iconName: String): ImageVector {
    return Icons.Default.Event
}

/**
 * State handling functions (to be implemented with proper ViewModel integration)
 */
@Composable
fun rememberDayEventsState(selectedDateTimestamp: Long, viewModel: MainViewModel): List<EventInstanceWithType> {
    val events by viewModel.eventInstancesForDate(selectedDateTimestamp).collectAsStateWithLifecycle(initialValue = emptyList())
    return events
}

@Composable
fun rememberActiveEventsState(viewModel: MainViewModel): List<EventInstanceWithType> {
    val events by viewModel.activeEvents.collectAsStateWithLifecycle(initialValue = emptyList())
    return events
}

@Composable
fun rememberIsLoadingState(viewModel: MainViewModel): Boolean {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false)
    return isLoading
}

/**
 * Formats the date for the day view header
 */
private fun formatDateHeader(timestampUtcMillis: Long): String {
    return DateTimeUtils.formatDate(timestampUtcMillis)
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

