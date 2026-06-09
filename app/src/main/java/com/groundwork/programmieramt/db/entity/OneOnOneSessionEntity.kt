package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "one_on_one_sessions",
    foreignKeys = [ForeignKey(
        entity = TeamMemberEntity::class,
        parentColumns = ["id"],
        childColumns = ["teamMemberId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("teamMemberId")]
)
data class OneOnOneSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teamMemberId: Long,
    val datum: Long = System.currentTimeMillis(),
    val sessionNumber: Int = 1,
    val strokes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
