package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.TeamNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamNoteDao {
    @Query("SELECT * FROM team_notes ORDER BY datum DESC")
    fun getAll(): Flow<List<TeamNoteEntity>>

    @Query("SELECT * FROM team_notes ORDER BY datum DESC")
    suspend fun getAllOnce(): List<TeamNoteEntity>

    @Query("SELECT * FROM team_notes WHERE id = :id")
    suspend fun getById(id: Long): TeamNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TeamNoteEntity): Long

    @Update
    suspend fun update(entity: TeamNoteEntity)

    @Delete
    suspend fun delete(entity: TeamNoteEntity)
}
