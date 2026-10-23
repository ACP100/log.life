package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Embedded
import com.example.loglife.data.model.Plan
import com.example.loglife.data.model.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Insert
    suspend fun insert(plan: Plan): Long

    @Update
    suspend fun update(plan: Plan)

    @Query("SELECT * FROM plans WHERE id = :planId")
    suspend fun getById(planId: Long): Plan?

    /** Pending plans planned within [start, end), earliest first, with their event. */
    @Query("""
        SELECT p.*,
            et.id AS type_id, et.name AS type_name, et.icon AS type_icon,
            et.eventMode AS type_eventMode, et.category AS type_category,
            et.showInWidget AS type_showInWidget, et.widgetOrder AS type_widgetOrder,
            et.color AS type_color, et.pinned AS type_pinned, et.archived AS type_archived,
            et.createdAt AS type_createdAt, et.updatedAt AS type_updatedAt
        FROM plans p
        JOIN event_types et ON p.eventId = et.id
        WHERE p.status = 'PENDING' AND p.plannedAt >= :start AND p.plannedAt < :end
        ORDER BY p.plannedAt
    """)
    fun getPendingBetweenFlow(start: Long, end: Long): Flow<List<PlanWithType>>

    @Query("""
        SELECT * FROM plans
        WHERE status = 'PENDING' AND eventId = :eventId AND plannedAt >= :start AND plannedAt < :end
        ORDER BY plannedAt
    """)
    suspend fun getPendingFor(eventId: Long, start: Long, end: Long): List<Plan>

    @Query("SELECT * FROM plans WHERE status = 'PENDING' AND plannedAt < :before")
    suspend fun getPendingBefore(before: Long): List<Plan>

    @Query("SELECT * FROM plans WHERE plannedAt >= :start AND plannedAt < :end ORDER BY plannedAt")
    suspend fun getBetween(start: Long, end: Long): List<Plan>

    @Query("DELETE FROM plans WHERE id = :planId")
    suspend fun deleteById(planId: Long)
}

data class PlanWithType(
    @Embedded val plan: Plan,
    @Embedded(prefix = "type_") val eventType: EventType
)
