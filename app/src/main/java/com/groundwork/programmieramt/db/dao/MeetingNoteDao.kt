package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingNoteDao {
    @Query("SELECT * FROM meeting_notes ORDER BY datum DESC")
    fun getAll(): Flow<List<MeetingNoteEntity>>

    @Query("SELECT * FROM meeting_notes ORDER BY datum DESC")
    suspend fun getAllOnce(): List<MeetingNoteEntity>

    @Query("SELECT * FROM meeting_notes WHERE id = :id")
    suspend fun getById(id: Long): MeetingNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeetingNoteEntity): Long

    @Update
    suspend fun update(entity: MeetingNoteEntity)

    @Delete
    suspend fun delete(entity: MeetingNoteEntity)
}
