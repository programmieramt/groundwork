package com.groundwork.programmieramt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.groundwork.programmieramt.db.dao.FreeNoteDao
import com.groundwork.programmieramt.db.dao.MeetingNoteDao
import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import com.groundwork.programmieramt.db.entity.FreeNoteEntity
import com.groundwork.programmieramt.db.entity.MeetingNoteEntity
import com.groundwork.programmieramt.db.entity.OneOnOneSessionEntity
import com.groundwork.programmieramt.db.entity.SofortNoteEntity
import com.groundwork.programmieramt.db.entity.TeamMemberEntity
import com.groundwork.programmieramt.db.entity.TeamNoteEntity

@Database(
    entities = [
        TeamMemberEntity::class,
        OneOnOneSessionEntity::class,
        TeamNoteEntity::class,
        SofortNoteEntity::class,
        FreeNoteEntity::class,
        MeetingNoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class GroundworkDatabase : RoomDatabase() {
    abstract fun teamMemberDao(): TeamMemberDao
    abstract fun oneOnOneSessionDao(): OneOnOneSessionDao
    abstract fun teamNoteDao(): TeamNoteDao
    abstract fun sofortNoteDao(): SofortNoteDao
    abstract fun freeNoteDao(): FreeNoteDao
    abstract fun meetingNoteDao(): MeetingNoteDao
}
