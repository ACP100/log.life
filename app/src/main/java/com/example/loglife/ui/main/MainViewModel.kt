package com.example.loglife.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the main/home screen.
 * Handles active events, recent events, and quick logging functionality.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val eventTypeRepository: EventTypeRepository,
    private val eventInstanceRepository: EventInstanceRepository
) : ViewModel() {

    // UI State
    private val _activeEvents = MutableStateFlow<List<EventInstanceWithType>>(emptyList())
    val activeEvents: StateFlow<List<EventInstanceWithType>> = _activeEvents.asStateFlow()

    private val _recentEvent = MutableStateFlow<EventInstanceWithType?>(null)
    val recentEvent: StateFlow<EventInstanceWithType?> = _recentEvent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _eventTypes = MutableStateFlow<List<EventType>>(emptyList())
    val eventTypes: StateFlow<List<EventType>> = _eventTypes.asStateFlow()

    // UI event flows for one-shot events (dialogs, navigation)
    private val _navigateToLogEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToLogEvent: SharedFlow<Unit> = _navigateToLogEvent.asSharedFlow()
    private val _showEventEditDialog = MutableSharedFlow<EventInstanceWithType>(extraBufferCapacity = 1)
    val showEventEditDialog: SharedFlow<EventInstanceWithType> = _showEventEditDialog.asSharedFlow()
    private val _showNoteEditDialog = MutableSharedFlow<EventInstanceWithType>(extraBufferCapacity = 1)
    val showNoteEditDialog: SharedFlow<EventInstanceWithType> = _showNoteEditDialog.asSharedFlow()

    // Drag/resize mode state
    private val _dragResizeEvent = MutableStateFlow<EventInstanceWithType?>(null)
    val dragResizeEvent: StateFlow<EventInstanceWithType?> = _dragResizeEvent.asStateFlow()
    private val _isDragResizeModeActive = MutableStateFlow<Boolean>(false)
    val isDragResizeModeActive: StateFlow<Boolean> = _isDragResizeModeActive.asStateFlow()

    fun eventInstancesForDate(timestamp: Long): Flow<List<EventInstanceWithType>> =
        eventInstanceRepository.getEventInstancesWithTypeForDate(timestamp)

    fun eventInstancesForDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<EventInstanceWithType>> =
        eventInstanceRepository.getEventInstancesWithTypeForDateRange(startTimestamp, endTimestamp)

    init {
        loadInitialData()
        loadEventTypes()
    }

    private fun loadEventTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            _eventTypes.value = eventTypeRepository.getAllEventTypes()
        }
    }

    /** Refreshes the event types list from the repository */
    fun refreshEventTypes() {
        loadEventTypes()
    }

    private fun loadInitialData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Load active duration events
                val activeEvents = eventInstanceRepository.getActiveDurationEvents()
                _activeEvents.value = activeEvents

                // Load most recent event (could be instant or duration)
                val allEvents = eventInstanceRepository.getAllEventInstances()
                if (allEvents.isNotEmpty()) {
                    val mostRecent = allEvents.first()
                    val eventWithType = eventInstanceRepository.getEventInstanceWithType(mostRecent.id)
                    _recentEvent.value = eventWithType
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logs an instant event (creates an EventInstance with endTime = null)
     */
    fun logInstantEvent(eventTypeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventInstance = com.example.loglife.data.model.EventInstance(
                eventTypeId = eventTypeId,
                startTime = DateTimeUtils.nowUtc()
                // endTime remains null for instant events
            )
            eventInstanceRepository.insertEventInstance(eventInstance)
            // Refresh recent event
            loadInitialData()
        }
    }

    /**
     * Starts a duration event (creates an EventInstance with endTime = null)
     */
    fun startDurationEvent(eventTypeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            // Check if there's already an active instance of this event type
            val activeEvents = eventInstanceRepository.getActiveDurationEvents()
            val alreadyActive = activeEvents.any { it.eventInstance.eventTypeId == eventTypeId }

            if (!alreadyActive) {
                val eventInstance = com.example.loglife.data.model.EventInstance(
                    eventTypeId = eventTypeId,
                    startTime = DateTimeUtils.nowUtc()
                    // endTime remains null for active duration
                )
                eventInstanceRepository.insertEventInstance(eventInstance)
                loadInitialData()
            }
            // If already active, do nothing (as per spec)
        }
    }

    /**
     * Stops an active duration event
     */
    fun stopDurationEvent(eventInstanceId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventInstance = eventInstanceRepository.getEventInstanceById(eventInstanceId)
            if (eventInstance == null) return@launch

            val stoppedInstance = eventInstance.copy(
                endTime = DateTimeUtils.nowUtc()
            )
            eventInstanceRepository.updateEventInstance(stoppedInstance)
            loadInitialData()
        }
    }

    /**
     * Toggles a duration event: starts if not active, stops if active
     */
    suspend fun toggleDurationEvent(eventTypeId: Long) {
        val activeEvents = eventInstanceRepository.getActiveDurationEvents()
        val activeInstance = activeEvents.firstOrNull {
            it.eventInstance.eventTypeId == eventTypeId
        }

        if (activeInstance != null) {
            // Stop the active event
            stopDurationEvent(activeInstance.eventInstance.id)
        } else {
            // Start the event
            startDurationEvent(eventTypeId)
        }
    }

    /**
     * Logs an event by id, choosing instant vs duration mode automatically.
     */
    fun logEvent(eventTypeId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventType = eventTypeRepository.getEventTypeById(eventTypeId) ?: return@launch
            when (eventType.eventMode) {
                com.example.loglife.data.model.EventMode.INSTANT -> logInstantEvent(eventTypeId)
                com.example.loglife.data.model.EventMode.DURATION -> startDurationEvent(eventTypeId)
            }
        }
    }

    /**
     * Gets the display text for an event based on its mode and state
     */
    fun getEventDisplayText(eventWithType: EventInstanceWithType): String {
        val eventInstance = eventWithType.eventInstance
        val eventType = eventWithType.eventType

        return when (eventType.eventMode) {
            com.example.loglife.data.model.EventMode.INSTANT -> {
                DateTimeUtils.formatTime(eventInstance.startTime)
            }
            com.example.loglife.data.model.EventMode.DURATION -> {
                if (eventInstance.endTime == null) {
                    // Active duration - show elapsed time
                    val elapsed = DateTimeUtils.formatDuration(
                        eventInstance.startTime,
                        DateTimeUtils.nowUtc()
                    )
                    return "$elapsed"
                } else {
                    // Completed duration - show start → end
                    val startTime = DateTimeUtils.formatTime(eventInstance.startTime)
                    val endTime = DateTimeUtils.formatTime(eventInstance.endTime!!)
                    return "$startTime → $endTime"
                }
            }
        }
    }

    /**
     * Gets the display name for an event (with active indicator if applicable)
     */
    fun getEventNameWithState(eventWithType: EventInstanceWithType): String {
        val eventInstance = eventWithType.eventInstance
        val eventType = eventWithType.eventType

        if (eventType.eventMode == com.example.loglife.data.model.EventMode.DURATION &&
            eventInstance.endTime == null) {
            return "${eventType.name} ●"  // ● indicates active
        }
        return eventType.name
    }

    // TODO-related functions for DayViewScreen
    fun startEventEditing(event: EventInstanceWithType) {
        // Show event edit dialog to modify event details (time, note, etc.)
        _showEventEditDialog.tryEmit(event)
    }

    fun openNoteEditor(event: EventInstanceWithType) {
        // Show note editor dialog to modify the event's note
        _showNoteEditDialog.tryEmit(event)
    }

    fun logQuickEvent(eventTypeId: Long) {
        // Implement quick log functionality:
        // For instant events: log the event (same as logEvent)
        // For duration events: toggle (start if not active, stop if active)
        // This makes it truly quick - one tap handles both start/stop cases
        viewModelScope.launch(Dispatchers.IO) {
            val eventType = eventTypeRepository.getEventTypeById(eventTypeId) ?: return@launch
            when (eventType.eventMode) {
                com.example.loglife.data.model.EventMode.INSTANT -> logInstantEvent(eventTypeId)
                com.example.loglife.data.model.EventMode.DURATION -> toggleDurationEvent(eventTypeId)
            }
        }
    }

    fun showLogEventDialog() {
        // Trigger navigation to the log event screen to select and log an event type
        _navigateToLogEvent.tryEmit(Unit)
    }

    fun startDragResizeMode(event: EventInstanceWithType) {
        // Enter drag/resize mode for adjusting event timing
        _isDragResizeModeActive.value = true
        _dragResizeEvent.value = event
    }

    /// Exit drag/resize mode without saving changes
    fun cancelDragResizeMode() {
        _isDragResizeModeActive.value = false
        _dragResizeEvent.value = null
    }

    /// Complete drag/resize operation and save changes
    fun finishDragResizeMode(
        newStartTime: Long? = null,
        newEndTime: Long? = null,
        newNote: String? = null
    ) {
        val eventToUpdate = _dragResizeEvent.value
        if (eventToUpdate == null) return

        viewModelScope.launch(Dispatchers.IO) {
            val eventInstance = eventToUpdate.eventInstance
            val updatedEvent = eventInstance.copy(
                startTime = newStartTime ?: eventInstance.startTime,
                endTime = newEndTime ?: eventInstance.endTime,
                note = newNote ?: eventInstance.note
            )
            eventInstanceRepository.updateEventInstance(updatedEvent)
            // Refresh UI state
            loadInitialData()
            // Exit drag/resize mode
            cancelDragResizeMode()
        }
    }

    /// Updates an event instance
    fun updateEventInstance(eventInstance: com.example.loglife.data.model.EventInstance) {
        viewModelScope.launch(Dispatchers.IO) {
            eventInstanceRepository.updateEventInstance(eventInstance)
            loadInitialData()
        }
    }

    /// Updates the note for an event instance
    fun updateEventNote(eventInstanceId: Long, note: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val eventInstance = eventInstanceRepository.getEventInstanceById(eventInstanceId) ?: return@launch
            val updatedEvent = eventInstance.copy(note = note)
            eventInstanceRepository.updateEventInstance(updatedEvent)
            loadInitialData()
        }
    }
}

/**
 * Factory for creating MainViewModel instances (for use with hiltViewModel())
 */
class MainViewModelFactory @Inject constructor(
    private val eventTypeRepository: EventTypeRepository,
    private val eventInstanceRepository: EventInstanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(eventTypeRepository, eventInstanceRepository) as T
    }
}