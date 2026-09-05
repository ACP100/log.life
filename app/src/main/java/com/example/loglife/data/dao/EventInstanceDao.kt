package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
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

    @Query("SELECT * FROM event_instances ORDER BY startTime DESC")
    suspend fun getAllEventInstances(): List<EventInstance>

    @Query("SELECT * FROM event_instances WHERE id = :instanceId")
    suspend fun getEventInstanceById(instanceId: Long): EventInstance?

    @Query("""
        SELECT * FROM event_instances
        WHERE date(startTime/1000, 'unixepoch') = date(:timestamp/1000, 'unixepoch')
        ORDER BY startTime
    """)
    suspend fun getEventInstancesForDate(timestamp: Long): List<EventInstance>

    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE date(ei.startTime/1000, 'unixepoch') = date(:timestamp/1000, 'unixepoch')
        ORDER BY ei.startTime
    """)
    fun getEventInstancesWithTypeForDate(timestamp: Long): Flow<List<EventInstanceWithType>>

    @Query("""
        SELECT * FROM event_instances
        WHERE startTime >= :startTimestamp
          AND startTime < :endTimestamp
        ORDER BY startTime
    """)
    suspend fun getEventInstancesForTimeRange(startTimestamp: Long, endTimestamp: Long): List<EventInstance>

    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.startTime >= :startTimestamp
          AND ei.startTime < :endTimestamp
        ORDER BY ei.startTime
    """)
    fun getEventInstancesWithTypeForDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<EventInstanceWithType>>

    @Query("""
        SELECT 
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon, 
            et.eventMode AS type_eventMode, et.category AS type_category, 
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.id = :instanceId
    """)
    suspend fun getEventInstanceWithType(instanceId: Long): EventInstanceWithType?

    @Insert
    suspend fun insert(eventInstance: EventInstance): Long

    @Insert
    suspend fun insertAll(eventInstances: List<EventInstance>): List<Long>

    @Update
    suspend fun update(eventInstance: EventInstance)

    @Delete
    suspend fun delete(eventInstance: EventInstance)

    @Query("DELETE FROM event_instances WHERE id = :instanceId")
    suspend fun deleteById(instanceId: Long)

    @Query("DELETE FROM event_instances")
    suspend fun deleteAll()

    // Active duration events (endTime is null)
    @Query("""
        SELECT
            ei.id, ei.eventTypeId, ei.startTime, ei.endTime, ei.note, ei.createdAt, ei.updatedAt,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM event_instances ei
        JOIN event_types et ON ei.eventTypeId = et.id
        WHERE ei.endTime IS NULL
        ORDER BY ei.startTime
    """)
    suspend fun getActiveDurationEvents(): List<EventInstanceWithType>
}

/**
 * Joined data class for EventInstance and EventType
 */
data class EventInstanceWithType(
    @Embedded val eventInstance: EventInstance,
    @Embedded(prefix = "type_") val eventType: EventType
)