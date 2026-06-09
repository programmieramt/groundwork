package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamMemberDao {
    @Query("SELECT * FROM team_members ORDER BY name ASC")
    fun getAll(): Flow<List<TeamMemberEntity>>

    @Query("SELECT * FROM team_members ORDER BY name ASC")
    suspend fun getAllOnce(): List<TeamMemberEntity>

    @Query("SELECT * FROM team_members WHERE id = :id")
    suspend fun getById(id: Long): TeamMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TeamMemberEntity): Long

    @Update
    suspend fun update(entity: TeamMemberEntity)

    @Delete
    suspend fun delete(entity: TeamMemberEntity)
}
