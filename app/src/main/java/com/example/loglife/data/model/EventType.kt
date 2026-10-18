package com.example.loglife.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a template for logging events (e.g., "Work", "Gym", "Coffee").
 * Event types are reusable definitions that users can instantiate multiple times.
 */
@Entity(tableName = "event_types",
        indices = [Index(value = ["name"], unique = true)])
data class EventType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String, // Material icon name or unicode character
    val eventMode: EventMode, // INSTANT or DURATION
    val category: String? = null,
    val showInWidget: Boolean = true,
    val widgetOrder: Int = 0,
    val color: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)