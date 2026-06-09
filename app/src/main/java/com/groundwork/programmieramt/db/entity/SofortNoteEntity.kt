package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "sofort_notes")
data class SofortNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val datum: Long = System.currentTimeMillis(),
    val kategorie: String = "",
    val capture: String = "",
    val followUp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
