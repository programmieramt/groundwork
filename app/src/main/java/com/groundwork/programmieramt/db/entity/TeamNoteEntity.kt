package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "team_notes")
data class TeamNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: Long = System.currentTimeMillis(),
    val kontextMeeting: String = "",
    val strokes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
