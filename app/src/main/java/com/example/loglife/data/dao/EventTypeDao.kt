package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.loglife.data.model.EventType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for EventType entities.
 * Provides methods for querying and manipulating event types.
 */
@Dao
interface EventTypeDao {

    @Query("SELECT * FROM event_types ORDER BY widgetOrder, name")
    suspend fun getAllEventTypes(): List<EventType>

    @Query("SELECT * FROM event_types WHERE id = :eventTypeId")
    suspend fun getEventTypeById(eventTypeId: Long): EventType?

    @Query("SELECT * FROM event_types WHERE showInWidget = 1 ORDER BY widgetOrder")
    suspend fun getWidgetEventTypes(): List<EventType>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventType: EventType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventTypes: List<EventType>): List<Long>

    @Update
    suspend fun update(eventType: EventType)

    @Delete
    suspend fun delete(eventType: EventType)

    @Query("DELETE FROM event_types")
    suspend fun deleteAll()
}