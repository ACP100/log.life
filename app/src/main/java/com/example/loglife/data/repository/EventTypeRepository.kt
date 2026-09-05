package com.example.loglife.data.repository

import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.model.EventType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing EventType data.
 * Handles data operations and provides a clean API for the ViewModel layer.
 */
@Singleton
class EventTypeRepository @Inject constructor(
    private val eventTypeDao: EventTypeDao
) {
    suspend fun getAllEventTypes(): List<EventType> = eventTypeDao.getAllEventTypes()

    suspend fun getEventTypeById(eventTypeId: Long): EventType? =
        eventTypeDao.getEventTypeById(eventTypeId)

    suspend fun getWidgetEventTypes(): List<EventType> = eventTypeDao.getWidgetEventTypes()

    suspend fun insertEventType(eventType: EventType): Long =
        eventTypeDao.insert(eventType)

    suspend fun insertAllEventTypes(eventTypes: List<EventType>): List<Long> =
        eventTypeDao.insertAll(eventTypes)

    suspend fun updateEventType(eventType: EventType) {
        eventTypeDao.update(eventType)
    }

    suspend fun deleteEventType(eventType: EventType) {
        eventTypeDao.delete(eventType)
    }

    suspend fun deleteAllEventTypes() {
        eventTypeDao.deleteAll()
    }
}