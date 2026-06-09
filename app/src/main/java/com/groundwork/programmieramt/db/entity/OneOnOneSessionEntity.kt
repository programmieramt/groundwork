package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val thema: String = "",
    val echteProblem: String = "",
    val vereinbarungen: String = "",
    val eindruck: String = "",
    val offenePunkte: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
