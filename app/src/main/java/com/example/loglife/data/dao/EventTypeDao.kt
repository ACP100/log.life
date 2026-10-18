package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM event_types ORDER BY widgetOrder, name")
    fun getAllEventTypesFlow(): Flow<List<EventType>>

    @Query("SELECT * FROM event_types WHERE id = :eventTypeId")
    suspend fun getEventTypeById(eventTypeId: Long): EventType?

    @Query("SELECT * FROM event_types WHERE showInWidget = 1 ORDER BY widgetOrder, name")
    suspend fun getWidgetEventTypes(): List<EventType>

    @Query("SELECT * FROM event_types WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getEventTypeByName(name: String): EventType?

    @Query("UPDATE event_types SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(oldName: String, newName: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(eventType: EventType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(eventTypes: List<EventType>): List<Long>

    @Update
    suspend fun update(eventType: EventType)

    @Query("DELETE FROM event_types")
    suspend fun deleteAll()
}