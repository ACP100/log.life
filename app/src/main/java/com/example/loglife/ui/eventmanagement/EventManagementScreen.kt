package com.example.loglife.ui.eventmanagement

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType
import com.example.loglife.ui.main.MainViewModel
import com.example.loglife.ui.widgetsslot.WidgetSlot
import com.example.loglife.ui.widgetsslot.WidgetSlotState
import com.example.loglife.util.DateTimeUtils
import com.example.loglife.util.DefaultEventTypes
import java.util.*

/**
 * Event management screen for creating, editing, and configuring event types.
 * Users can define new event types, set their properties, and configure widget settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventManagementScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventManagementViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val eventTypes by viewModel.eventTypes.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<Long?>(null) }
    var editedEvent by remember {
        mutableStateOf(
            EventType(
                name = "",
                icon = "event",
                eventMode = EventMode.INSTANT,
                category = "",
                showInWidget = true,
                widgetOrder = 0
            )
        )
    }

    fun startEditing(eventType: EventType) {
        editingEventId = eventType.id
        editedEvent = eventType.copy()
        showAddDialog = true
    }

    fun resetEditForm() {
        editingEventId = null
        editedEvent = EventType(
            name = "",
            icon = "event",
            eventMode = EventMode.INSTANT,
            category = "",
            showInWidget = true,
            widgetOrder = 0
        )
        showAddDialog = true
    }

    fun saveEvent(eventType: EventType) {
        val isEditing = editingEventId != null
        coroutineScope.launch {
            val success = viewModel.saveEventType(
                eventType = if (isEditing) eventType else eventType.copy(id = 0),
                isEditing = isEditing
            )
            if (success) {
                showAddDialog = false
                editingEventId = null
                // Refresh event types in MainViewModel so new event types appear in lists
                mainViewModel.refreshEventTypes()
            }
        }
    }

    fun deleteEvent(eventType: EventType) {
        coroutineScope.launch {
            viewModel.deleteEventType(eventType)
            // Refresh event types in MainViewModel so deleted event types are removed from lists
            mainViewModel.refreshEventTypes()
        }
    }

    fun loadDefaults() {
        coroutineScope.launch {
            val defaultTypes = DefaultEventTypes.getDefaultEventTypes()
            for (eventType in defaultTypes) {
                viewModel.saveEventType(eventType, false)
            }
            // Refresh event types in MainViewModel so default event types appear in lists
            mainViewModel.refreshEventTypes()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Event Management") },
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
                EventManagementContent(
                    eventTypes = eventTypes,
                    showAddDialog = showAddDialog,
                    onAddDialogClose = { showAddDialog = false },
                    onAddEventClick = { resetEditForm() },
                    onEditEventClick = { eventType -> startEditing(eventType) },
                    onDeleteEventClick = { eventType -> deleteEvent(eventType) },
                    onSaveEventClick = { event -> saveEvent(event) },
                    onLoadDefaultsClick = { loadDefaults() },
                    editedEvent = editedEvent,
                    onEditedEventChange = { editedEvent = it },
                    editingEventId = editingEventId,
                    modifier = contentModifier
                )
            }
        }
    )
}

@Composable
fun EventManagementContent(
    eventTypes: List<EventType>,
    showAddDialog: Boolean,
    onAddDialogClose: () -> Unit,
    onAddEventClick: () -> Unit,
    onEditEventClick: (EventType) -> Unit,
    onDeleteEventClick: (EventType) -> Unit,
    onSaveEventClick: (EventType) -> Unit,
    onLoadDefaultsClick: () -> Unit,
    editedEvent: EventType,
    onEditedEventChange: (EventType) -> Unit,
    editingEventId: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // Header with add button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Event Types",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onAddEventClick
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add event type"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Event types list
        if (eventTypes.isEmpty()) {
            EmptyEventTypesState(
                onAddEventClick = onAddEventClick,
                onLoadDefaultsClick = onLoadDefaultsClick
            )
        } else {
            EventTypesList(
                eventTypes = eventTypes,
                onEditClick = onEditEventClick,
                onDeleteClick = onDeleteEventClick,
                onSaveClick = onSaveEventClick
            )
        }

        // Add/Edit dialog
        if (showAddDialog) {
            EventTypeDialog(
                eventType = editedEvent,
                onChange = { onEditedEventChange(it) },
                onSaveClick = { onSaveEventClick(editedEvent) },
                onCancelClick = { onAddDialogClose() },
                isEditing = editingEventId != null
            )
        }
    }
}

@Composable
fun EmptyEventTypesState(
    onAddEventClick: () -> Unit,
    onLoadDefaultsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.EventAvailable,
            contentDescription = "No event types",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No event types defined yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first event type to start logging",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onAddEventClick
            ) {
                Text("Add Event Type")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onLoadDefaultsClick
            ) {
                Text("Load Defaults")
            }
        }
    }
}

@Composable
fun EventTypesList(
    eventTypes: List<EventType>,
    onEditClick: (EventType) -> Unit,
    onDeleteClick: (EventType) -> Unit,
    onSaveClick: (EventType) -> Unit
) {
    // Group by category for better organization
    val groupedEvents = eventTypes.groupBy { it.category ?: "Uncategorized" }

    val sections = groupedEvents.toList()
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        sections.forEachIndexed { sectionIndex, sectionItem ->
            val (category, eventsInCategory) = sectionItem

            // Section header
            if (eventsInCategory.isNotEmpty()) {
                item {
                    SectionHeader(
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
                    onEditClick = { onEditClick(eventType) },
                    onDeleteClick = { onDeleteClick(eventType) },
                    onChange = onSaveClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = if (eventIndex == 0 && sectionIndex > 0) 16.dp else 8.dp,
                            horizontal = 16.dp
                        )
                )
            }

            // Divider between sections (except after last section)
            if (sectionIndex < sections.lastIndex) {
                item {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
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
            text = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
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
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onChange: (EventType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWidgetVisible = eventType.showInWidget

    Card(
        modifier = modifier
            .clickable { onEditClick() }
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
                Text(
                    text = when (eventType.eventMode) {
                        EventMode.INSTANT -> "Instant"
                        EventMode.DURATION -> "Duration"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Widget visibility toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show in widget",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isWidgetVisible,
                    onCheckedChange = { onChange(eventType.copy(showInWidget = it)) },
                    enabled = true
                )
            }

            // Delete button
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeDialog(
    eventType: EventType,
    onChange: (EventType) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    isEditing: Boolean = false
) {
    val iconName = eventType.icon
    val eventMode = eventType.eventMode
    val category = eventType.category ?: ""
    val showInWidget = eventType.showInWidget
    val widgetOrder = eventType.widgetOrder
    val canSave = eventType.name.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onCancelClick,
        confirmButton = {
            TextButton(
                onClick = onSaveClick,
                enabled = canSave
            ) {
                Text(if (isEditing) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelClick
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(if (isEditing) "Edit Event Type" else "Add Event Type")
        },
        text = {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                TextField(
                    label = { Text("Event Name") },
                    value = eventType.name,
                    onValueChange = {
                        onChange(eventType.copy(name = it))
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Icon selector (simplified)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Icon",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = getIconResource(iconName),
                        contentDescription = "Selected icon",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            Log.d("EventManagementScreen", "Icon picker clicked - not implemented yet")
                            // TODO: Implement actual icon picker dialog
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change icon"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Event mode selector
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Event Type",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = eventMode == EventMode.INSTANT,
                            onClick = { onChange(eventType.copy(eventMode = EventMode.INSTANT)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Instant")
                        }
                        SegmentedButton(
                            selected = eventMode == EventMode.DURATION,
                            onClick = { onChange(eventType.copy(eventMode = EventMode.DURATION)) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Duration")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Category
                TextField(
                    label = { Text("Category (optional)") },
                    value = category,
                    onValueChange = {
                        onChange(eventType.copy(category = it.takeIf { it.isNotBlank() }))
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Widget settings
                Text(
                    text = "Widget Settings",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Show in Widget",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showInWidget,
                        onCheckedChange = {
                            onChange(eventType.copy(showInWidget = it))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (showInWidget) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Widget Order",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Simple number input (in reality would use a proper number field)
                        TextField(
                            label = { Text("Order") },
                            value = widgetOrder.toString(),
                            onValueChange = {
                                val order = it.toIntOrNull() ?: 0
                                onChange(eventType.copy(widgetOrder = order))
                            },
                            singleLine = true
                        )
                    }
                }
            }
        })
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
    return when (iconName.lowercase()) {
        "event" -> Icons.Default.Event
        "settings" -> Icons.Default.Settings
        "favorite" -> Icons.Default.Favorite
        "alarm" -> Icons.Default.Alarm
        "notification" -> Icons.Default.Notifications
        "search" -> Icons.Default.Search
        "add" -> Icons.Default.Add
        "delete" -> Icons.Default.Delete
        "edit" -> Icons.Default.Edit
        "share" -> Icons.Default.Share
        "upload" -> Icons.Default.CloudUpload
        else -> Icons.Default.Event // Default fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
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