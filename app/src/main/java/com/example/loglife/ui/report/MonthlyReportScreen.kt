package com.example.loglife.ui.report

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.util.ActivityStats
import com.example.loglife.util.CategoryStats
import com.example.loglife.util.DateTimeUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    viewModel: MainViewModel = hiltViewModel(),
    selectedMonthTimestamp: Long,
    onMonthChanged: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(DateTimeUtils.formatMonth(selectedMonthTimestamp)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                EmptyState(selectedMonthTimestamp)
            }
        }
    }
}

@Composable
fun MonthlyReportSection(
    title: String,
    icon: String,
    data: CategoryStats,
    isEmpty: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = getIconResource(icon), contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (!isEmpty) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f hours total", data.totalHours),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(text = "No data", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ActivityBreakdown(
    activities: List<ActivityStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "Activity Breakdown", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        activities.forEach { activity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = activity.name)
                Text(text = String.format(Locale.getDefault(), "%.1f h", activity.totalHours))
            }
        }
    }
}

@Composable
fun EmptyState(monthStart: Long, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No events logged for ${DateTimeUtils.formatMonth(monthStart)}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun getIconResource(iconName: String): ImageVector {
    return when (iconName) {
        "nightlight_round" -> Icons.Default.NightlightRound
        "work" -> Icons.Default.Work
        "fitness_center" -> Icons.Default.FitnessCenter
        "school" -> Icons.Default.School
        "spa" -> Icons.Default.Spa
        else -> Icons.Default.Event
    }
}
