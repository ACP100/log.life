package com.example.loglife.data.repository

import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.dao.EventInstanceWithType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing EventInstance data.
 * Handles data operations and provides a clean API for the ViewModel layer.
 */
@Singleton
class EventInstanceRepository @Inject constructor(
    private val eventInstanceDao: EventInstanceDao
) {
    suspend fun getAllEventInstances(): List<EventInstance> =
        eventInstanceDao.getAllEventInstances()

    suspend fun getEventInstanceById(instanceId: Long): EventInstance? =
        eventInstanceDao.getEventInstanceById(instanceId)

    suspend fun getEventInstancesForDate(timestampUtcMillis: Long): List<EventInstance> =
        eventInstanceDao.getEventInstancesForDate(timestampUtcMillis)

    fun getEventInstancesWithTypeForDate(timestampUtcMillis: Long): kotlinx.coroutines.flow.Flow<List<EventInstanceWithType>> =
        eventInstanceDao.getEventInstancesWithTypeForDate(timestampUtcMillis)

    fun getEventInstancesWithTypeForDateRange(
        startTimestampUtcMillis: Long,
        endTimestampUtcMillis: Long
    ): kotlinx.coroutines.flow.Flow<List<EventInstanceWithType>> =
        eventInstanceDao.getEventInstancesWithTypeForDateRange(startTimestampUtcMillis, endTimestampUtcMillis)

    suspend fun getEventInstancesForTimeRange(
        startTimestampUtcMillis: Long,
        endTimestampUtcMillis: Long
    ): List<EventInstance> =
        eventInstanceDao.getEventInstancesForTimeRange(
            startTimestampUtcMillis, endTimestampUtcMillis
        )

    suspend fun getEventInstanceWithType(instanceId: Long): EventInstanceWithType? =
        eventInstanceDao.getEventInstanceWithType(instanceId)

    suspend fun insertEventInstance(eventInstance: EventInstance): Long =
        eventInstanceDao.insert(eventInstance)

    suspend fun insertAllEventInstances(eventInstances: List<EventInstance>): List<Long> =
        eventInstanceDao.insertAll(eventInstances)

    suspend fun updateEventInstance(eventInstance: EventInstance) {
        eventInstanceDao.update(eventInstance)
    }

    suspend fun deleteEventInstance(eventInstance: EventInstance) {
        eventInstanceDao.delete(eventInstance)
    }

    suspend fun deleteEventInstanceById(instanceId: Long) {
        eventInstanceDao.deleteById(instanceId)
    }

    suspend fun deleteAllEventInstances() {
        eventInstanceDao.deleteAll()
    }

    // Active duration events (endTime is null)
    suspend fun getActiveDurationEvents(): List<EventInstanceWithType> =
        eventInstanceDao.getActiveDurationEvents()
}