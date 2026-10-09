package com.example.loglife.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.loglife.R
import com.example.loglife.data.model.EventInstance
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

data class CalendarDayData(
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val eventsForDay: List<EventInstance>
)

/**
 * Calendar screen for navigating through days and months.
 * Shows a month-view calendar with visual indicators for days that have logged events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onDateSelected: (Long) -> Unit, // timestamp in milliseconds for start of day
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonthState = remember { mutableStateOf(getCurrentMonthStart()) }
    val currentMonth = currentMonthState.value
    val eventsByDate = rememberEventsByDateState(viewModel, currentMonthState)
    val isLoading = rememberIsLoadingState(viewModel)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Calendar") },
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
                CalendarContent(
                    currentMonth = currentMonth,
                    eventsByDate = eventsByDate,
                    onDateClick = { timestamp ->
                        onDateSelected(timestamp)
                    },
                    onPreviousMonth = {
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
                        calendar.timeInMillis = currentMonth
                        calendar.add(Calendar.MONTH, -1)
                        currentMonthState.value = calendar.timeInMillis
                    },
                    onNextMonth = {
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
                        calendar.timeInMillis = currentMonth
                        calendar.add(Calendar.MONTH, 1)
                        currentMonthState.value = calendar.timeInMillis
                    },
                    modifier = contentModifier
                )
            }
        }
    )
}

@Composable
fun CalendarContent(
    currentMonth: Long, // Start of month in UTC milliseconds
    eventsByDate: Map<Long, List<EventInstance>>, // date -> events
    onDateClick: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calendarData = generateCalendarData(currentMonth, eventsByDate)
    val monthName = getMonthName(currentMonth)
    val year = getYear(currentMonth)

    Column(
        modifier = modifier
    ) {
        // Month header
        MonthHeader(
            monthName = monthName,
            year = year,
            onPreviousMonthClick = onPreviousMonth,
            onNextMonthClick = onNextMonth
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Weekday headers
        WeekdayHeaders()
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            
        ) {
            items(calendarData.size / 7) { weekIndex ->
                val startIndex = weekIndex * 7
                val endIndex = startIndex + 7
                val weekDays = calendarData.subList(startIndex, endIndex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    weekDays.forEach { dayData ->
                        DayCell(
                            dayOfMonth = dayData.dayOfMonth,
                            isCurrentMonth = dayData.isCurrentMonth,
                            isToday = dayData.isToday,
                            eventCount = dayData.eventsForDay.size,
                            onClick = {
                                if (dayData.isCurrentMonth) {
                                    val dateTimestamp = getDateTimestamp(currentMonth, dayData.dayOfMonth)
                                    onDateClick(dateTimestamp)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(
    monthName: String,
    year: Int,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonthClick
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous month"
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = year.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
        }
        IconButton(
            onClick = onNextMonthClick
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
fun WeekdayHeaders() {
    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        weekdays.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun DayCell(
    dayOfMonth: Int,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    eventCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (isCurrentMonth) 1.0f else 0.3f
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else if (eventCount > 0) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .alpha(contentAlpha)
            .border(
                width = if (isToday) 2.dp else 0.5.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            // Day number
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Event indicator dots
            if (eventCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    repeat(min(eventCount, 3)) { index ->
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        )
                        if (index < 2) Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (eventCount > 3) {
                        Text(
                            text = "+${eventCount - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
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

/**
 * Helper functions for calendar calculations
 */
private fun getCurrentMonthStart(): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = System.currentTimeMillis()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getMonthName(timestamp: Long): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = timestamp
    return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
}

private fun getYear(timestamp: Long): Int {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = timestamp
    return calendar.get(Calendar.YEAR)
}

private fun getDateTimestamp(monthStart: Long, dayOfMonth: Int): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = monthStart
    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
    return calendar.timeInMillis
}

/**
 * Generates calendar data for a given month
 * Returns list of: CalendarDayData
 */
private fun generateCalendarData(monthStart: Long, eventsByDate: Map<Long, List<EventInstance>>): List<CalendarDayData> {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = monthStart

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, etc.

    val today = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    today.timeInMillis = System.currentTimeMillis()
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    val todayMonth = today.get(Calendar.MONTH)
    val todayYear = today.get(Calendar.YEAR)

    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val month = calendar.get(Calendar.MONTH)
    val year = calendar.get(Calendar.YEAR)

    val calendarData = mutableListOf<CalendarDayData>()

    // Add padding days from previous month
    val daysToPad = firstDayOfWeek - 1 // Sunday=0, Monday=1, ..., Saturday=6
    for (i in 0 until daysToPad) {
        calendarData.add(CalendarDayData(0, false, false, emptyList()))
    }

    // Add days of current month
    for (day in 1..daysInMonth) {
        val isCurrent = true
        val isToday = (day == todayDay && month == todayMonth && year == todayYear)
        val dateTimestamp = getDateTimestamp(monthStart, day)
        val eventsForDay = eventsByDate[dateTimestamp] ?: emptyList()
        calendarData.add(CalendarDayData(day, isCurrent, isToday, eventsForDay))
    }

    // Add padding days to complete the last week
    val remainingDays = calendarData.size % 7
    if (remainingDays != 0) {
        for (i in 0 until (7 - remainingDays)) {
            calendarData.add(CalendarDayData(0, false, false, emptyList()))
        }
    }

    return calendarData
}


@Composable
fun rememberEventsByDateState(viewModel: MainViewModel, currentMonth: MutableState<Long>): Map<Long, List<EventInstance>> {
    // Calculate the start and end of the current month
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = currentMonth.value
    val startOfMonth = calendar.timeInMillis

    calendar.add(Calendar.MONTH, 1)
    calendar.add(Calendar.DAY_OF_MONTH, -1)
    val endOfMonth = calendar.timeInMillis

    // Collect events for the current month from the ViewModel
    val eventsForMonth = viewModel.eventInstancesForDateRange(
        startOfMonth,
        endOfMonth + DateTimeUtils.DAY_IN_MILLIS  // Add one day to include the last day fully
    ).collectAsStateWithLifecycle(initialValue = emptyList()).value

    // Group events by date (start of day)
    val groupedEvents = mutableMapOf<Long, MutableList<EventInstance>>()
    for (eventWithType in eventsForMonth) {
        val startOfDay = DateTimeUtils.getStartOfDay(eventWithType.eventInstance.startTime)
        val eventInstance = eventWithType.eventInstance
        val existingEvents = groupedEvents.getOrPut(startOfDay) { mutableListOf() }
        existingEvents.add(eventInstance)
    }

    return groupedEvents
}

@Composable
fun rememberIsLoadingState(viewModel: MainViewModel): Boolean {
    return viewModel.isLoading.collectAsStateWithLifecycle().value
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
