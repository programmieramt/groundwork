package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SofortNoteDao {
    @Query("SELECT * FROM sofort_notes ORDER BY datum DESC")
    fun getAll(): Flow<List<SofortNoteEntity>>

    @Query("SELECT * FROM sofort_notes ORDER BY datum DESC")
    suspend fun getAllOnce(): List<SofortNoteEntity>

    @Query("SELECT * FROM sofort_notes WHERE id = :id")
    suspend fun getById(id: Long): SofortNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SofortNoteEntity): Long

    @Update
    suspend fun update(entity: SofortNoteEntity)

    @Delete
    suspend fun delete(entity: SofortNoteEntity)
}
