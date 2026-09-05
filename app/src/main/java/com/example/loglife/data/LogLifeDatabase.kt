package com.example.loglife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType

/**
 * Room database for the life logging app.
 * Contains tables for event types and event instances.
 */
@Database(
    entities = [EventType::class, EventInstance::class],
    version = 3 ,
    exportSchema = false
)
abstract class LogLifeDatabase : RoomDatabase() {

    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventInstanceDao(): EventInstanceDao

    companion object {
        @Volatile
        private var INSTANCE: LogLifeDatabase? = null

        fun getInstance(context: Context): LogLifeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): LogLifeDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                LogLifeDatabase::class.java,
                "loglife_database"
            )
                .fallbackToDestructiveMigration() // For simplicity in v1
                .build()
        }
    }
}