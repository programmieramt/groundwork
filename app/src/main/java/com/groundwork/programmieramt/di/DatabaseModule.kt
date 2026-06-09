package com.groundwork.programmieramt.di

import android.content.Context
import androidx.room.Room
import com.groundwork.programmieramt.db.GroundworkDatabase
import com.groundwork.programmieramt.db.dao.OneOnOneSessionDao
import com.groundwork.programmieramt.db.dao.SofortNoteDao
import com.groundwork.programmieramt.db.dao.TeamMemberDao
import com.groundwork.programmieramt.db.dao.TeamNoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GroundworkDatabase =
        Room.databaseBuilder(context, GroundworkDatabase::class.java, "groundwork.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideTeamMemberDao(db: GroundworkDatabase): TeamMemberDao = db.teamMemberDao()
    @Provides fun provideOneOnOneSessionDao(db: GroundworkDatabase): OneOnOneSessionDao = db.oneOnOneSessionDao()
    @Provides fun provideTeamNoteDao(db: GroundworkDatabase): TeamNoteDao = db.teamNoteDao()
    @Provides fun provideSofortNoteDao(db: GroundworkDatabase): SofortNoteDao = db.sofortNoteDao()
}
