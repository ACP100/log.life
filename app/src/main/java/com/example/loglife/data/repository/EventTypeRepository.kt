package com.example.loglife.data.repository

import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.model.EventType
import kotlinx.coroutines.flow.Flow
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

    fun getAllEventTypesFlow(): Flow<List<EventType>> = eventTypeDao.getAllEventTypesFlow()

    suspend fun getEventTypeById(eventTypeId: Long): EventType? =
        eventTypeDao.getEventTypeById(eventTypeId)

    suspend fun getWidgetEventTypes(): List<EventType> = eventTypeDao.getWidgetEventTypes()

    suspend fun getEventTypeByName(name: String): EventType? = eventTypeDao.getEventTypeByName(name)

    suspend fun renameCategory(oldName: String, newName: String?) =
        eventTypeDao.renameCategory(oldName, newName)

    suspend fun insertEventType(eventType: EventType): Long =
        eventTypeDao.insert(eventType)

    suspend fun updateEventType(eventType: EventType) {
        eventTypeDao.update(eventType)
    }
}