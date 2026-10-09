package com.example.loglife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.viewmodel.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.data.dao.EventInstanceWithType
import kotlinx.coroutines.flow.map
import java.util.*
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var timeframe by remember { mutableStateOf(Timeframe.Month) }

    val now = DateTimeUtils.nowUtc()
    val (startTime, endTime) = when (timeframe) {
        Timeframe.Day -> DateTimeUtils.getStartOfDay(now) to DateTimeUtils.getEndOfDay(now)
        Timeframe.Week -> DateTimeUtils.getStartOfWeek(now) to DateTimeUtils.getEndOfWeek(now)
        Timeframe.Month -> DateTimeUtils.getStartOfMonth(now) to DateTimeUtils.getEndOfMonth(now)
        Timeframe.Year -> DateTimeUtils.getStartOfYear(now) to DateTimeUtils.getEndOfYear(now)
    }

    val events by viewModel.eventInstancesForDateRange(startTime, endTime)
        .collectAsState(initial = emptyList<EventInstanceWithType>())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    StatsScreenContent(
        timeframe = timeframe,
        events = events,
        isLoading = isLoading,
        startTime = startTime,
        onTimeframeChange = { timeframe = it }
    )
}

/**
 * ViewModel-free statistics content: selector + stats or empty state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreenContent(
    timeframe: Timeframe,
    events: List<EventInstanceWithType>,
    isLoading: Boolean,
    startTime: Long,
    onTimeframeChange: (Timeframe) -> Unit,
    modifier: Modifier = Modifier
) {

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Statistics") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                CenteredLoadingIndicator()
            } else {
                TimeframeSelector(
                    selectedTimeframe = timeframe,
                    onTimeframeClick = onTimeframeChange
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Simple statistics display
                if (events.isNotEmpty()) {
                    SimpleStatistics(events = events, startTime = startTime)
                } else {
                    EmptyStatsState()
                }
            }
        }
    }
}

@Composable
private fun TimeframeSelector(
    selectedTimeframe: Timeframe,
    onTimeframeClick: (Timeframe) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Timeframe.values().forEach { tf ->
            Button(
                onClick = { onTimeframeClick(tf) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTimeframe == tf) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Text(tf.name)
            }
        }
    }
}

@Composable
private fun SimpleStatistics(events: List<EventInstanceWithType>, startTime: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Text(
            text = "${DateTimeUtils.formatDate(startTime)} Statistics",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Total events
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Total Events", style = MaterialTheme.typography.labelMedium)
                Text(text = events.size.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline,
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Days with events
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = "Active Days", style = MaterialTheme.typography.labelMedium)
                // Calculate unique days
                val uniqueDays = events
                    .map { it.eventInstance.startTime }
                    .map { DateTimeUtils.getStartOfDay(it) }
                    .distinct()
                    .size
                Text(text = "$uniqueDays days", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun EmptyStatsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventAvailable,
            contentDescription = "No events recorded",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events recorded yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start logging events to see statistics",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CenteredLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

enum class Timeframe {
    Day, Week, Month, Year
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun StatsScreenPreview() {
    // Never call StatsScreen() here: it resolves MainViewModel via hiltViewModel(),
    // which crashes the preview. Render the stateless content with sample data instead.
    val now = DateTimeUtils.nowUtc()
    val sampleEvents = listOf(
        EventInstanceWithType(
            eventInstance = EventInstance(id = 1, eventTypeId = 1, startTime = now - 2 * 60 * 60 * 1000),
            eventType = EventType(id = 1, name = "Work", icon = "work", eventMode = EventMode.DURATION, category = "Work")
        ),
        EventInstanceWithType(
            eventInstance = EventInstance(id = 2, eventTypeId = 2, startTime = now - 5 * 60 * 60 * 1000),
            eventType = EventType(id = 2, name = "Coffee", icon = "coffee", eventMode = EventMode.INSTANT, category = "Food")
        )
    )
    LoglifeTheme {
        StatsScreenContent(
            timeframe = Timeframe.Month,
            events = sampleEvents,
            isLoading = false,
            startTime = DateTimeUtils.getStartOfMonth(now),
            onTimeframeChange = {}
        )
    }
}