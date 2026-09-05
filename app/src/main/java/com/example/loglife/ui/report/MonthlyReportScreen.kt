package com.example.loglife.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.InsertChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.ui.widgetsslot.WidgetSlot
import com.example.loglife.ui.widgetsslot.WidgetSlotState
import java.util.*

/**
 * Monthly report screen showing aggregated insights and statistics for a selected month.
 * Focuses on reflection rather than gamification - shows what actually happened without judgment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: MainViewModel = hiltViewModel(),
    selectedMonthTimestamp: Long, // Start of month in UTC milliseconds
    onMonthChanged: (Long) -> Unit, // Callback for month navigation
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthStart = remember(selectedMonthTimestamp) { getMonthStart(selectedMonthTimestamp) }
    val monthEnd = remember(selectedMonthTimestamp) { getMonthEnd(selectedMonthTimestamp) }
    val reportData = rememberReportDataState(viewModel, monthStart, monthEnd)
    val isLoading = rememberIsLoadingState(viewModel)

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(formatMonthHeader(monthStart)) },
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
            } else {
                MonthlyReportContent(
                    monthStart = monthStart,
                    monthEnd = monthEnd,
                    reportData = reportData,
                    onMonthChanged = onMonthChanged,
                    onNavigateBack = onNavigateBack,
                    modifier = contentModifier
                )
            }
        }
    )
}

@Composable
fun MonthlyReportContent(
    monthStart: Long,
    monthEnd: Long,
    reportData: MonthlyReportData,
    onMonthChanged: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Month navigation header
        MonthNavigationHeader(
            monthStart = monthStart,
            onPreviousMonthClick = {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
                calendar.timeInMillis = monthStart
                calendar.add(Calendar.MONTH, -1)
                onMonthChanged(calendar.timeInMillis)
            },
            onNextMonthClick = {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
                calendar.timeInMillis = monthStart
                calendar.add(Calendar.MONTH, 1)
                onMonthChanged(calendar.timeInMillis)
            }
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Summary statistics
        SummaryStatistics(
            reportData = reportData,
            monthStart = monthStart
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Detailed sections
        if (reportData.totalEvents > 0) {
            MonthlyReportSection(
                title = "Sleep",
                icon = "nightlight_round",
                data = reportData.sleepStats,
                isEmpty = reportData.sleepStats.totalHours == 0.0
            )
            Spacer(modifier = Modifier.height(16.dp))

            MonthlyReportSection(
                title = "Work",
                icon = "work",
                data = reportData.workStats,
                isEmpty = reportData.workStats.totalHours == 0.0
            )
            Spacer(modifier = Modifier.height(16.dp))

            MonthlyReportSection(
                title = "Health",
                icon = "fitness_center",
                data = reportData.healthStats,
                isEmpty = reportData.healthStats.totalHours == 0.0
            )
            Spacer(modifier = Modifier.height(16.dp))

            MonthlyReportSection(
                title = "Learning",
                icon = "school",
                data = reportData.learningStats,
                isEmpty = reportData.learningStats.totalHours == 0.0
            )
            Spacer(modifier = Modifier.height(16.dp))

            MonthlyReportSection(
                title = "Leisure",
                icon = "spa",
                data = reportData.leisureStats,
                isEmpty = reportData.leisureStats.totalHours == 0.0
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Activity breakdown
            ActivityBreakdown(
                activities = reportData.activities
            )
        } else {
            EmptyState(monthStart = monthStart)
        }
    }
}

@Composable
fun MonthNavigationHeader(
    monthStart: Long,
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
                text = getMonthName(monthStart),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = getYear(monthStart).toString(),
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
fun SummaryStatistics(reportData: MonthlyReportData, monthStart: Long) {
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
            text = "Monthly Summary",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = "Total events",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Total Events",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = reportData.totalEvents.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Days logged",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Days Logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${reportData.daysLogged}/${getDaysInMonth(monthStart)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MonthlyReportSection(
    title: String,
    icon: String,
    data: CategoryStats,
    isEmpty: Boolean = false
) {
    if (isEmpty && data.totalHours == 0.0 && data.eventCount == 0) {
        return  // Skip empty sections
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = formatDuration(data.totalHours),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${data.eventCount} sessions",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Sessions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (data.eventCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Average: ${formatDuration(data.averageHoursPerSession)} per session",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActivityBreakdown(activities: List<ActivityStats>) {
    if (activities.isEmpty()) return

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
            text = "Activity Breakdown",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(activities) { activity ->
                ActivityItem(
                    activity = activity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ActivityItem(
    activity: ActivityStats,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(12.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        // Icon
        Icon(
            imageVector = getIconResource(activity.icon),
            contentDescription = activity.name,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        // Activity info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (activity.category != null) {
                Text(
                    text = activity.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stats
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${activity.eventCount}x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(activity.totalHours),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(monthStart: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.InsertChart,
            contentDescription = "No data available",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No events logged for ${getMonthName(monthStart)} ${getYear(monthStart)}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start logging events to see your monthly insights",
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

/**
 * Data classes for report data
 */
data class MonthlyReportData(
    val totalEvents: Int = 0,
    val daysLogged: Int = 0,
    val sleepStats: CategoryStats = CategoryStats(),
    val workStats: CategoryStats = CategoryStats(),
    val healthStats: CategoryStats = CategoryStats(),
    val learningStats: CategoryStats = CategoryStats(),
    val leisureStats: CategoryStats = CategoryStats(),
    val activities: List<ActivityStats> = emptyList()
)

data class CategoryStats(
    val totalHours: Double = 0.0,
    val eventCount: Int = 0,
    val averageHoursPerSession: Double = 0.0
)

data class ActivityStats(
    val name: String = "",
    val icon: String = "",
    val category: String? = null,
    val totalHours: Double = 0.0,
    val eventCount: Int = 0
)

private fun getIconResource(iconName: String): ImageVector {
    return Icons.Default.Event
}

private fun calculateReportData(
    events: List<EventInstanceWithType>,
    monthStart: Long,
    monthEnd: Long
): MonthlyReportData {
    // Filter events for the selected month
    val monthEvents = events.filter { event ->
        val eventStart = event.eventInstance.startTime
        eventStart >= monthStart && eventStart < monthEnd
    }

    if (monthEvents.isEmpty()) {
        return MonthlyReportData()
    }

    // Calculate unique days logged
    val uniqueDays = monthEvents
        .map { DateTimeUtils.getStartOfDay(it.eventInstance.startTime) }
        .distinct()
        .size

    // Calculate stats by category
    val categoryEvents = groupEventsByCategory(monthEvents)

    val sleepStats = calculateCategoryStats(categoryEvents["Sleep"] ?: emptyList(), "Daily Routine")
    val workStats = calculateCategoryStats(categoryEvents["Work"] ?: emptyList(), "Work")
    val healthStats = calculateCategoryStats(
        (categoryEvents["Health"] ?: emptyList()) +
                (categoryEvents["Exercise"] ?: emptyList())
    )
    val learningStats = calculateCategoryStats(
        (categoryEvents["Learning"] ?: emptyList()) +
                (categoryEvents["Reading"] ?: emptyList())
    )
    val leisureStats = calculateCategoryStats(
        (categoryEvents["Leisure"] ?: emptyList()) +
                (categoryEvents["Social"] ?: emptyList()) +
                (categoryEvents["Gaming"] ?: emptyList()) +
                (categoryEvents["Watching TV"] ?: emptyList()) +
                (categoryEvents["Watching Movie"] ?: emptyList()) +
                (categoryEvents["Music"] ?: emptyList()) +
                (categoryEvents["Relaxing"] ?: emptyList())
    )

    // Individual activity stats
    val activities = monthEvents
        .groupBy { it.eventType.name }
        .map { (name, events) ->
            val totalHours = events.sumOf {
                val durationMs = if (it.eventInstance.endTime != null) {
                    it.eventInstance.endTime!! - it.eventInstance.startTime
                } else {
                    0L // Instant events don't contribute to duration
                }
                durationMs / (60.0 * 60 * 1000) // Convert to hours
            }
            val eventCount = events.size
            val firstEvent = events.firstOrNull()
            ActivityStats(
                name = name,
                icon = firstEvent?.eventType?.icon ?: "",
                category = firstEvent?.eventType?.category,
                totalHours = totalHours,
                eventCount = eventCount
            )
        }
        .sortedByDescending { it.totalHours }

    return MonthlyReportData(
        totalEvents = monthEvents.size,
        daysLogged = uniqueDays,
        sleepStats = sleepStats,
        workStats = workStats,
        healthStats = healthStats,
        learningStats = learningStats,
        leisureStats = leisureStats,
        activities = activities
    )
}

private fun groupEventsByCategory(events: List<EventInstanceWithType>): Map<String, List<EventInstanceWithType>> {
    val map = HashMap<String, MutableList<EventInstanceWithType>>()
    for (event in events) {
        val category = event.eventType.category ?: "Uncategorized"
        if (!map.containsKey(category)) {
            map[category] = mutableListOf()
        }
        map[category]!!.add(event)
    }
    return map.mapValues { (key, value) -> value.toList() }
}

private fun calculateCategoryStats(
    events: List<EventInstanceWithType>,
    categoryName: String? = null
): CategoryStats {
    if (events.isEmpty()) return CategoryStats()

    val totalDurationMs = events.sumOf {
        val durationMs = if (it.eventInstance.endTime != null) {
            it.eventInstance.endTime!! - it.eventInstance.startTime
        } else {
            0L // Instant events don't contribute to duration stats
        }
        durationMs
    }
    val totalHours = totalDurationMs / (60.0 * 60 * 1000)
    val eventCount = events.size
    val averageHoursPerSession = if (eventCount > 0) totalHours / eventCount else 0.0

    return CategoryStats(
        totalHours = totalHours,
        eventCount = eventCount,
        averageHoursPerSession = averageHoursPerSession
    )
}

/**
 * Helper functions for date formatting
 */
private fun getMonthStart(timestamp: Long): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun getMonthEnd(timestamp: Long): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    return calendar.timeInMillis
}

private fun getDaysInMonth(timestamp: Long): Int {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.getDefault())
    calendar.timeInMillis = timestamp
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
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

private fun formatDuration(hours: Double): String {
    val totalHours = hours.toInt()
    val minutes = ((hours - totalHours) * 60).toInt()

    return when {
        totalHours >= 24 -> {
            val days = totalHours / 24
            val remainingHours = totalHours % 24
            if (remainingHours > 0) {
                "$days d $remainingHours h"
            } else {
                "$days d"
            }
        }
        totalHours >= 1 -> {
            val minutesPart = if (minutes > 0) " $minutes m" else ""
            "$totalHours h$minutesPart"
        }
        else -> {
            if (minutes > 0) "$minutes m" else "0 m"
        }
    }
}

/**
 * State handling functions (to be implemented with proper ViewModel integration)
 */
@Composable
fun rememberReportDataState(viewModel: MainViewModel, monthStart: Long, monthEnd: Long): MonthlyReportData {
    val events = viewModel.eventInstancesForDateRange(monthStart, monthEnd)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    return calculateReportData(events, monthStart, monthEnd)
}

@Composable
fun rememberIsLoadingState(viewModel: MainViewModel): Boolean {
    return viewModel.isLoading.collectAsStateWithLifecycle(initialValue = false).value
}

/**
 * Formats the month for the report header
 */
private fun formatMonthHeader(timestampUtcMillis: Long): String {
    return "${getMonthName(timestampUtcMillis)} ${getYear(timestampUtcMillis)}"
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
