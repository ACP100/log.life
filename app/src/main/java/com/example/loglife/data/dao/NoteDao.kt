package com.example.loglife.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.loglife.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(note: Note): Long

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Query("SELECT * FROM notes ORDER BY createdAt")
    fun getAllFlow(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE logId = :logId ORDER BY createdAt")
    suspend fun forLog(logId: Long): List<Note>

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteById(noteId: Long)
}
