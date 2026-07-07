package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "one_on_one_sessions")
data class OneOnOneSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val memberName: String = "",
    val datum: Long = System.currentTimeMillis(),
    val titel: String = "",
    val strokes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
