package com.example.loglife.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.dao.NoteDao
import com.example.loglife.data.dao.PlanDao
import com.example.loglife.data.model.EventInstance
import com.example.loglife.data.model.EventType
import com.example.loglife.data.model.Note
import com.example.loglife.data.model.Plan
import com.example.loglife.util.DefaultEventTypes
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/**
 * Room database for the life logging app.
 * Contains tables for event types and event instances.
 */
@Database(
    entities = [EventType::class, EventInstance::class, Note::class, Plan::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LogLifeDatabase : RoomDatabase() {

    abstract fun eventTypeDao(): EventTypeDao
    abstract fun eventInstanceDao(): EventInstanceDao
    abstract fun noteDao(): NoteDao
    abstract fun planDao(): PlanDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event_types ADD COLUMN color TEXT")
                db.execSQL("ALTER TABLE event_types ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE event_types ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE event_instances ADD COLUMN planId INTEGER")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `logId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `text` TEXT NOT NULL,
                        FOREIGN KEY(`logId`) REFERENCES `event_instances`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_logId` ON `notes` (`logId`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` INTEGER NOT NULL,
                        `plannedAt` INTEGER NOT NULL,
                        `remindOffsetMin` INTEGER,
                        `repeatDays` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `startedLogId` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`eventId`) REFERENCES `event_types`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_eventId` ON `plans` (`eventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_plannedAt` ON `plans` (`plannedAt`)")
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration() // For simplicity in v1
                .build()
        }
    }
}