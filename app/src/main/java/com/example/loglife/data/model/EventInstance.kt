package com.example.loglife.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single logged occurrence of an event type.
 * For instant events: endTime is null.
 * For duration events: endTime is null when active, set when stopped.
 */
@Entity(tableName = "event_instances",
        indices = [
            Index(value = ["startTime"]),
            Index(value = ["eventTypeId"])
        ],
        foreignKeys = [
            ForeignKey(
                entity = EventType::class,
                parentColumns = ["id"],
                childColumns = ["eventTypeId"],
                onDelete = ForeignKey.CASCADE
            )
        ]
)
data class EventInstance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventTypeId: Long,
    val startTime: Long, // UTC timestamp in milliseconds
    val endTime: Long? = null, // null for instant events or active durations
    val note: String? = null,
    val planId: Long? = null, // set when this log was started from a Plan
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
