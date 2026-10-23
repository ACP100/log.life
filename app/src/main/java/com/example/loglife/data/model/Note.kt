package com.example.loglife.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A short plain-text note attached to a logged instance. Deleted together with its log. */
@Entity(
    tableName = "notes",
    indices = [Index(value = ["logId"])],
    foreignKeys = [
        ForeignKey(
            entity = EventInstance::class,
            parentColumns = ["id"],
            childColumns = ["logId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val logId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val text: String
)
