package com.groundwork.programmieramt.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.groundwork.programmieramt.db.entity.OneOnOneSession

@Database(entities = [OneOnOneSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun oneOnOneDao(): OneOnOneDao
}
