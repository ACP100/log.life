package com.example.loglife.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.loglife.data.LogLifeDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Export, restore and wipe: the whole-database operations behind Settings → Data. */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LogLifeDatabase
) {
    enum class Format(val mimeType: String, val extension: String) {
        CSV("text/csv", "csv"),
        JSON("application/json", "json"),
    }

    private suspend fun snapshot() = BackupData(
        eventTypes = database.eventTypeDao().getAllEventTypes(),
        logs = database.eventInstanceDao().getAll(),
    )

    /** Writes the export to a document the user picked. */
    suspend fun export(uri: Uri, format: Format) = withContext(Dispatchers.IO) {
        val data = snapshot()
        val text = when (format) {
            Format.CSV -> BackupCodec.toCsv(data)
            Format.JSON -> BackupCodec.toJson(data, System.currentTimeMillis())
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: error("Couldn't open the file")
    }

    /** Reads and checks a backup without touching the database. */
    suspend fun read(uri: Uri): BackupData = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: throw BackupFormatException("Couldn't open the file.")
        BackupCodec.fromJson(text)
    }

    /** Replaces everything on this phone with [data], in one transaction. */
    suspend fun restore(data: BackupData) = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.eventInstanceDao().deleteAll()
            database.eventTypeDao().deleteAll()
            database.eventTypeDao().insertAll(data.eventTypes)
            database.eventInstanceDao().insertAll(data.logs)
        }
    }

    suspend fun deleteEverything() = withContext(Dispatchers.IO) {
        database.withTransaction {
            database.eventInstanceDao().deleteAll()
            database.eventTypeDao().deleteAll()
        }
    }

    fun suggestedFileName(format: Format, now: java.time.LocalDate = java.time.LocalDate.now()) =
        "loglife-$now.${format.extension}"
}
