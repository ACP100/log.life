package com.example.loglife.ui.eventmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loglife.data.model.EventType
import com.example.loglife.data.repository.EventTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the event management screen.
 * Manages CRUD operations for event types backed by a Room repository,
 * so changes persist across screen re-entry.
 */
@HiltViewModel
class EventManagementViewModel @Inject constructor(
    private val repository: EventTypeRepository
) : ViewModel() {

    private val _eventTypes = MutableStateFlow<List<EventType>>(emptyList())
    val eventTypes: StateFlow<List<EventType>> = _eventTypes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadEventTypes()
    }

    fun loadEventTypes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                _eventTypes.value = repository.getAllEventTypes()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Inserts a new event type or updates an existing one.
     * Returns true on success, or false if the name collides with another type.
     */
    suspend fun saveEventType(eventType: EventType, isEditing: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val trimmedName = eventType.name.trim()
            val duplicate = _eventTypes.value.firstOrNull {
                it.name.equals(trimmedName, ignoreCase = true)
            }
            if (duplicate != null && (isEditing.not() || duplicate.id != eventType.id)) {
                return@withContext false
            }

            if (isEditing) {
                repository.updateEventType(
                    eventType.copy(name = trimmedName, updatedAt = System.currentTimeMillis())
                )
            } else {
                repository.insertEventType(
                    eventType.copy(
                        id = 0,
                        name = trimmedName,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            loadEventTypes()
            true
        }
    }

    suspend fun deleteEventType(eventType: EventType) {
        withContext(Dispatchers.IO) {
            repository.deleteEventType(eventType)
            loadEventTypes()
        }
    }
}
