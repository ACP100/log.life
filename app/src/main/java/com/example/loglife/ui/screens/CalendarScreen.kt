package com.example.loglife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.R
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.viewmodel.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.data.model.EventMode
import com.example.loglife.ui.calendar.CalendarContent
import com.example.loglife.ui.calendar.CalendarGrid
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

/**
 * Calendar screen showing month view with day detail when a date is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var selectedDate by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calendar") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Month view calendar (real implementation)
            CalendarGrid(
                viewModel = viewModel,
                onDateSelected = { date ->
                    selectedDate = date
                },
                onNavigateBack = {
                    // Navigate back to previous screen (e.g., clear selected date)
                    selectedDate = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Day detail section
            selectedDate?.let { date ->
                val startOfDay = DateTimeUtils.getStartOfDay(date)
                val endOfDay = DateTimeUtils.getEndOfDay(date)
                val events by viewModel.eventInstancesForDateRange(startOfDay, endOfDay)
                    .collectAsStateWithLifecycle(initialValue = emptyList())
                CalendarDayDetailSection(
                    date = date,
                    events = events
                )
            }
        }
    }
}

/**
 * ViewModel-free day detail: event list for a selected date, or an empty state.
 */
@Composable
fun CalendarDayDetailSection(
    date: Long,
    events: List<EventInstanceWithType>,
    modifier: Modifier = Modifier
) {
    if (events.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Events for ${DateTimeUtils.formatDate(date)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(events) { eventWithType ->
                    val event = eventWithType.eventInstance
                    val eventType = eventWithType.eventType
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
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
                                Text(
                                    text = DateTimeUtils.formatTime(event.startTime),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No events for ${DateTimeUtils.formatDate(date)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun CalendarScreenPreview() {
    // Never call CalendarScreen() here: it resolves MainViewModel via hiltViewModel(),
    // which crashes the preview. Render the stateless pieces with sample data instead.
    val now = DateTimeUtils.nowUtc()
    val monthStart = DateTimeUtils.getStartOfMonth(now)
    val sampleDate = DateTimeUtils.getStartOfDay(now)
    val sampleEvents = listOf(
        EventInstanceWithType(
            eventInstance = EventInstance(id = 1, eventTypeId = 1, startTime = now - 60 * 60 * 1000),
            eventType = EventType(id = 1, name = "Coffee", icon = "coffee", eventMode = EventMode.INSTANT, category = "Food")
        )
    )
    LoglifeTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Calendar") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                CalendarContent(
                    currentMonth = monthStart,
                    eventsByDate = mapOf(sampleDate to sampleEvents.map { it.eventInstance }),
                    onDateClick = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    modifier = Modifier.fillMaxWidth()
                )
                CalendarDayDetailSection(
                    date = sampleDate,
                    events = sampleEvents
                )
            }
        }
    }
}
