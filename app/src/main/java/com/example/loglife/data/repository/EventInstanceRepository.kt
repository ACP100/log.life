package com.example.loglife.data.repository

import android.content.Context
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventInstanceWithType
import com.example.loglife.data.dao.EventTypeStats
import com.example.loglife.data.dao.NoteDao
import com.example.loglife.data.dao.PlanDao
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.Note
import com.example.loglife.data.model.PlanStatus
import com.example.loglife.data.settings.ConcurrentPolicy
import com.example.loglife.notifications.PlanReminders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Result of [EventInstanceRepository.startSession]. */
data class SessionStart(
    val instanceId: Long,
    /** Still-running session to offer to pause; only set under [ConcurrentPolicy.ASK]. */
    val otherRunning: EventInstanceWithType?
)

/** A deleted log and its notes, kept so an Undo can put them back exactly. */
data class DeletedLog(val instance: EventInstance, val notes: List<Note>)

/**
 * Repository for managing EventInstance data.
 * Handles data operations and provides a clean API for the ViewModel layer.
 */
@Singleton
class EventInstanceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventInstanceDao: EventInstanceDao,
    private val planDao: PlanDao,
    private val noteDao: NoteDao
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

    /**
     * Inserts a log. If a plan for the same event is pending on the log's day,
     * the log fulfils it (any start route counts).
     */
    suspend fun insertEventInstance(eventInstance: EventInstance): Long {
        val plan = findPendingPlan(eventInstance.eventTypeId, eventInstance.startTime)
        val id = eventInstanceDao.insert(eventInstance.copy(planId = plan?.id))
        if (plan != null) markPlanStarted(plan.id, id)
        return id
    }

    fun getLogCountFlow(): Flow<Int> = eventInstanceDao.countFlow()

    suspend fun updateEventInstance(eventInstance: EventInstance) {
        eventInstanceDao.update(eventInstance)
    }

    /** Deletes a log; its notes go with it (foreign key cascade). Returns what is needed to undo. */
    suspend fun deleteEventInstanceById(instanceId: Long): DeletedLog? {
        val instance = eventInstanceDao.getEventInstanceById(instanceId) ?: return null
        val notes = noteDao.forLog(instanceId)
        eventInstanceDao.deleteById(instanceId)
        return DeletedLog(instance, notes)
    }

    /** Puts back a log deleted with [deleteEventInstanceById], notes included. */
    suspend fun restoreLog(deleted: DeletedLog) {
        eventInstanceDao.insert(deleted.instance)
        if (deleted.notes.isNotEmpty()) noteDao.insertAll(deleted.notes)
    }

    /** Sets the start and (for sessions) end of a log. Overlaps are allowed. */
    suspend fun updateTimes(instanceId: Long, start: Long, end: Long?, now: Long) {
        val instance = eventInstanceDao.getEventInstanceById(instanceId) ?: return
        eventInstanceDao.update(instance.copy(startTime = start, endTime = end, updatedAt = now))
    }

    // ---- Notes ----

    suspend fun addNote(logId: Long, text: String, now: Long = System.currentTimeMillis()) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        noteDao.insert(Note(logId = logId, createdAt = now, text = clean))
    }

    fun getAllNotesFlow(): Flow<List<Note>> = noteDao.getAllFlow()

    // ---- Plans ----

    /**
     * Starts a session of [eventTypeId], applying the "another one is running"
     * setting. Returns null when that type is already running. When [planId] is
     * given the new log fulfils that plan; otherwise a pending plan for today does.
     */
    suspend fun startSession(
        eventTypeId: Long,
        now: Long,
        policy: ConcurrentPolicy,
        planId: Long? = null
    ): SessionStart? {
        val running = getRunningSessions()
        if (running.any { it.eventInstance.eventTypeId == eventTypeId }) return null
        if (policy == ConcurrentPolicy.STOP_OTHER) {
            running.forEach { eventInstanceDao.update(it.eventInstance.copy(endTime = now, updatedAt = now)) }
        }
        val plan = planId?.let { planDao.getById(it)?.takeIf { p -> p.status == PlanStatus.PENDING } }
            ?: findPendingPlan(eventTypeId, now)
        val id = eventInstanceDao.insert(
            EventInstance(eventTypeId = eventTypeId, startTime = now, planId = plan?.id)
        )
        if (plan != null) markPlanStarted(plan.id, id)
        val other = if (policy == ConcurrentPolicy.ASK) running.maxByOrNull { it.eventInstance.startTime } else null
        return SessionStart(id, other)
    }

    /** Ends every running session of [eventTypeId] (used before archiving it). */
    suspend fun stopRunningOfType(eventTypeId: Long, now: Long) {
        getRunningSessions()
            .filter { it.eventInstance.eventTypeId == eventTypeId }
            .forEach { eventInstanceDao.update(it.eventInstance.copy(endTime = now, updatedAt = now)) }
    }

    /** Running sessions only (instant events also store endTime == null). */
    suspend fun getRunningSessions(): List<EventInstanceWithType> =
        eventInstanceDao.getActiveDurationEvents().filter {
            it.eventType.eventMode == EventMode.DURATION && it.eventInstance.endTime == null
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

    /** Logs of one event within [start, end), oldest first. */
    suspend fun getLogsForTypeBetween(eventTypeId: Long, start: Long, end: Long): List<EventInstance> =
        eventInstanceDao.getForTypeBetween(eventTypeId, start, end)

    private suspend fun findPendingPlan(eventTypeId: Long, atMillis: Long) =
        dayRange(atMillis).let { (start, end) -> planDao.getPendingFor(eventTypeId, start, end).firstOrNull() }

    private suspend fun markPlanStarted(planId: Long, logId: Long) {
        val plan = planDao.getById(planId) ?: return
        planDao.update(plan.copy(status = PlanStatus.STARTED, startedLogId = logId))
        PlanReminders.cancel(context, planId)
    }

    private fun dayRange(millis: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val day = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        return dayStart(day, zone) to dayStart(day.plusDays(1), zone)
    }

    private fun dayStart(day: LocalDate, zone: ZoneId): Long =
        day.atStartOfDay(zone).toInstant().toEpochMilli()
}
