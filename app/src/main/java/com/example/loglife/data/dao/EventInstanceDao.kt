package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Embedded
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for EventInstance entities.
 * Provides methods for querying and manipulating event instances.
 */
@Dao
interface EventInstanceDao {

    @Query("SELECT * FROM event_instances WHERE id = :instanceId")
    suspend fun getEventInstanceById(instanceId: Long): EventInstance?

    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.startTime >= :startTimestamp
          AND ei.startTime < :endTimestamp
        ORDER BY ei.startTime
    """)
    fun getEventInstancesWithTypeForDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<EventInstanceWithType>>

    // Every log that touches [start, end): moments inside it, sessions that
    // overlap it (including ones that began earlier), and sessions still running.
    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.startTime < :endTimestamp
          AND (
            ei.startTime >= :startTimestamp
            OR ei.endTime > :startTimestamp
            OR (ei.endTime IS NULL AND et.eventMode = 'DURATION')
          )
        ORDER BY ei.startTime
    """)
    fun getEventInstancesWithTypeOverlapping(startTimestamp: Long, endTimestamp: Long): Flow<List<EventInstanceWithType>>

    // One-shot variant for widgets (no Flow collection in a broadcast receiver).
    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.startTime < :endTimestamp
          AND (
            ei.startTime >= :startTimestamp
            OR ei.endTime > :startTimestamp
            OR (ei.endTime IS NULL AND et.eventMode = 'DURATION')
          )
        ORDER BY ei.startTime
    """)
    suspend fun getEventInstancesWithTypeOverlappingSync(startTimestamp: Long, endTimestamp: Long): List<EventInstanceWithType>

    @Insert
    suspend fun insert(eventInstance: EventInstance): Long

    @Insert
    suspend fun insertAll(eventInstances: List<EventInstance>)

    @Query("SELECT * FROM event_instances ORDER BY startTime")
    suspend fun getAll(): List<EventInstance>

    @Query("SELECT COUNT(*) FROM event_instances")
    fun countFlow(): Flow<Int>

    @Query("DELETE FROM event_instances")
    suspend fun deleteAll()

    @Update
    suspend fun update(eventInstance: EventInstance)

    @Query("DELETE FROM event_instances WHERE id = :instanceId")
    suspend fun deleteById(instanceId: Long)

    // Active duration events (endTime is null)
    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.endTime IS NULL
        ORDER BY ei.startTime
    """)
    suspend fun getActiveDurationEvents(): List<EventInstanceWithType>

    // Reactive variant: widget -> app sync relies on this Flow.
    // NOTE: endTime IS NULL matches both active durations AND instant events
    // (instant events also store endTime = null). Callers that only want
    // durations must filter by eventMode == DURATION.
    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.endTime IS NULL
        ORDER BY ei.startTime
    """)
    fun getActiveDurationEventsFlow(): Flow<List<EventInstanceWithType>>

    @Query("""
        SELECT eventTypeId,
               COUNT(*) AS total,
               SUM(CASE WHEN startTime >= :since THEN 1 ELSE 0 END) AS recent,
               MAX(startTime) AS lastStart
        FROM event_instances
        GROUP BY eventTypeId
    """)
    fun getEventTypeStatsFlow(since: Long): Flow<List<EventTypeStats>>

    @Query("""
        SELECT eventTypeId,
               COUNT(*) AS total,
               SUM(CASE WHEN startTime >= :since THEN 1 ELSE 0 END) AS recent,
               MAX(startTime) AS lastStart
        FROM event_instances
        GROUP BY eventTypeId
    """)
    suspend fun getEventTypeStats(since: Long): List<EventTypeStats>
}

data class EventTypeStats(
    val eventTypeId: Long,
    val total: Int,
    val recent: Int,
    val lastStart: Long
)

/**
 * Joined data class for EventInstance and EventType
 */
data class EventInstanceWithType(
    @Embedded val eventInstance: EventInstance,
    @Embedded(prefix = "type_") val eventType: EventType
)