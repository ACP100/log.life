package com.example.loglife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.util.DefaultEventTypes
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/**
 * Room database for the life logging app.
 * Contains tables for event types and event instances.
 */
@Database(
    entities = [EventType::class, EventInstance::class],
    version = 4,
    exportSchema = false
)
abstract class LogLifeDatabase : RoomDatabase() {

    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventInstanceDao(): EventInstanceDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event_types ADD COLUMN color TEXT")
                db.execSQL("ALTER TABLE event_types ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE event_types ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }

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
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {
                            try {
                                val database = getInstance(context)
                                val defaultTypes = DefaultEventTypes.getDefaultEventTypes()
                                runBlocking {
                                    database.eventTypeDao().insertAll(defaultTypes)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration() // For simplicity in v1
                .build()
        }
    }
}