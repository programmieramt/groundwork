package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FreeNoteDao {
    @Query("SELECT * FROM free_notes ORDER BY datum DESC")
    fun getAll(): Flow<List<FreeNoteEntity>>

    @Query("SELECT * FROM free_notes ORDER BY datum DESC")
    suspend fun getAllOnce(): List<FreeNoteEntity>

    @Query("SELECT * FROM free_notes WHERE id = :id")
    suspend fun getById(id: Long): FreeNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FreeNoteEntity): Long

    @Update
    suspend fun update(entity: FreeNoteEntity)

    @Delete
    suspend fun delete(entity: FreeNoteEntity)
}
