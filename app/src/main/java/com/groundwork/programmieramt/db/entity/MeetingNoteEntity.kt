package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "meeting_notes")
data class MeetingNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: Long = System.currentTimeMillis(),
    val teilnehmer: String = "",
    val strokes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
