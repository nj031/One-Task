package com.nj031.onetask.data.journal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: JournalNoteEntity)

    @Update
    suspend fun update(note: JournalNoteEntity)

    @Delete
    suspend fun delete(note: JournalNoteEntity)

    @Query("SELECT * FROM journal_notes WHERE id = :id")
    suspend fun getById(id: String): JournalNoteEntity?

    @Query("SELECT * FROM journal_notes")
    fun getAll(): Flow<List<JournalNoteEntity>>

    @Query("SELECT * FROM journal_notes WHERE status = :status")
    fun getByStatus(status: JournalNoteStatus): Flow<List<JournalNoteEntity>>
}
