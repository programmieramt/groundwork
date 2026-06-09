package com.groundwork.programmieramt.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rolle: String = "",
    val team: String = "",
    val erstkontakt: Long = 0L,
    val ersterEindruck: String = "",
    val staerken: String = "",
    val entwicklungsfeld: String = "",
    val motivation: String = "",
    val sensibles: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
