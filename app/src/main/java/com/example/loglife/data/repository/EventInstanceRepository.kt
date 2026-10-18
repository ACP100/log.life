package com.example.loglife.data.repository

import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.settings.ConcurrentPolicy
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Result of [EventInstanceRepository.startSession]. */
data class SessionStart(
    val instanceId: Long,
    /** Still-running session to offer to pause; only set under [ConcurrentPolicy.ASK]. */
    val otherRunning: EventInstanceWithType?
)

/**
 * Repository for managing EventInstance data.
 * Handles data operations and provides a clean API for the ViewModel layer.
 */
@Singleton
class EventInstanceRepository @Inject constructor(
    private val eventInstanceDao: EventInstanceDao
) {
    suspend fun getEventInstanceById(instanceId: Long): EventInstance? =
        eventInstanceDao.getEventInstanceById(instanceId)

    fun getEventInstancesWithTypeForDateRange(
        startTimestampUtcMillis: Long,
        endTimestampUtcMillis: Long
    ): Flow<List<EventInstanceWithType>> =
        eventInstanceDao.getEventInstancesWithTypeForDateRange(startTimestampUtcMillis, endTimestampUtcMillis)

    fun getEventInstancesWithTypeOverlapping(startMillis: Long, endMillis: Long): Flow<List<EventInstanceWithType>> =
        eventInstanceDao.getEventInstancesWithTypeOverlapping(startMillis, endMillis)

    suspend fun insertEventInstance(eventInstance: EventInstance): Long =
        eventInstanceDao.insert(eventInstance)

    fun getLogCountFlow(): Flow<Int> = eventInstanceDao.countFlow()

    suspend fun updateEventInstance(eventInstance: EventInstance) {
        eventInstanceDao.update(eventInstance)
    }

    suspend fun deleteEventInstanceById(instanceId: Long) {
        eventInstanceDao.deleteById(instanceId)
    }

    /** Running sessions only (instant events also store endTime == null). */
    suspend fun getRunningSessions(): List<EventInstanceWithType> =
        eventInstanceDao.getActiveDurationEvents().filter {
            it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null
        }

    /**
     * Starts a session of [eventTypeId], applying the "another one is running"
     * setting. Returns null when that type is already running.
     */
    suspend fun startSession(eventTypeId: Long, now: Long, policy: ConcurrentPolicy): SessionStart? {
        val running = getRunningSessions()
        if (running.any { it.eventInstance.eventTypeId == eventTypeId }) return null
        if (policy == ConcurrentPolicy.STOP_OTHER) {
            running.forEach { eventInstanceDao.update(it.eventInstance.copy(endTime = now, updatedAt = now)) }
        }
        val id = eventInstanceDao.insert(EventInstance(eventTypeId = eventTypeId, startTime = now))
        val other = if (policy == ConcurrentPolicy.ASK) running.maxByOrNull { it.eventInstance.startTime } else null
        return SessionStart(id, other)
    }

    /** Ends every running session of [eventTypeId] (used before archiving it). */
    suspend fun stopRunningOfType(eventTypeId: Long, now: Long) {
        getRunningSessions()
            .filter { it.eventInstance.eventTypeId == eventTypeId }
            .forEach { eventInstanceDao.update(it.eventInstance.copy(endTime = now, updatedAt = now)) }
    }

    // Active duration events (endTime is null)
    suspend fun getActiveDurationEvents(): List<EventInstanceWithType> =
        eventInstanceDao.getActiveDurationEvents()

    fun getActiveDurationEventsFlow(): Flow<List<EventInstanceWithType>> =
        eventInstanceDao.getActiveDurationEventsFlow()

    suspend fun getEventInstancesWithTypeOverlappingSync(startMillis: Long, endMillis: Long): List<EventInstanceWithType> =
        eventInstanceDao.getEventInstancesWithTypeOverlappingSync(startMillis, endMillis)

    suspend fun getEventTypeStats(since: Long = 0L): List<EventTypeStats> =
        eventInstanceDao.getEventTypeStats(since)

    fun getEventTypeStatsFlow(since: Long): Flow<List<EventTypeStats>> =
        eventInstanceDao.getEventTypeStatsFlow(since)
}
