package com.example.loglife.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlanStatus { PENDING, STARTED, MISSED }

/**
 * A log the user intends to start at [plannedAt].
 * [remindOffsetMin] is minutes after [plannedAt] to nudge (null = no reminder).
 * [repeatDays] are ISO weekdays (1 = Mon … 7 = Sun); empty means a one-off.
 * Each repeating plan is stored as one row per matching day.
 */
@Entity(
    tableName = "plans",
    indices = [Index(value = ["eventId"]), Index(value = ["plannedAt"])],
    foreignKeys = [
        ForeignKey(
            entity = EventType::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val plannedAt: Long,
    val remindOffsetMin: Int? = 10,
    val repeatDays: List<Int> = emptyList(),
    val status: PlanStatus = PlanStatus.PENDING,
    val startedLogId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
