package com.groundwork.programmieramt.db.dao

import androidx.room.*
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OneOnOneSessionDao {
    @Query("SELECT * FROM one_on_one_sessions ORDER BY datum DESC")
    fun getAll(): Flow<List<OneOnOneSessionEntity>>

    @Query("SELECT * FROM one_on_one_sessions WHERE teamMemberId = :memberId ORDER BY datum DESC")
    fun getByMember(memberId: Long): Flow<List<OneOnOneSessionEntity>>

    @Query("SELECT * FROM one_on_one_sessions WHERE id = :id")
    suspend fun getById(id: Long): OneOnOneSessionEntity?

    @Query("SELECT COUNT(*) FROM one_on_one_sessions WHERE teamMemberId = :memberId")
    suspend fun countByMember(memberId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OneOnOneSessionEntity): Long

    @Update
    suspend fun update(entity: OneOnOneSessionEntity)

    @Delete
    suspend fun delete(entity: OneOnOneSessionEntity)
}
