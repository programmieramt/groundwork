package com.groundwork.programmieramt.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.groundwork.programmieramt.db.entity.OneOnOneSession
import kotlinx.coroutines.flow.Flow

@Dao
interface OneOnOneDao {
    @Query("SELECT * FROM one_on_one_sessions ORDER BY datum DESC")
    fun getAllFlow(): Flow<List<OneOnOneSession>>

    @Query("SELECT * FROM one_on_one_sessions WHERE id = :id")
    suspend fun getById(id: Long): OneOnOneSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: OneOnOneSession): Long

    @Query("DELETE FROM one_on_one_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
