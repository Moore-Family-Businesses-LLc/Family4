package com.family4.app.di

import android.content.Context
import androidx.room.Room
import com.family4.app.data.db.Family4Database
import com.family4.app.data.db.dao.*
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
    fun provideDatabase(@ApplicationContext context: Context): Family4Database =
        Room.databaseBuilder(context, Family4Database::class.java, "family4.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideNoteDao(db: Family4Database): NoteDao = db.noteDao()
    @Provides fun provideChatDao(db: Family4Database): ChatDao = db.chatDao()
    @Provides fun provideMemberDao(db: Family4Database): FamilyMemberDao = db.memberDao()
    @Provides fun provideCalendarDao(db: Family4Database): CalendarDao = db.calendarDao()
    @Provides fun provideTaskDao(db: Family4Database): TaskDao = db.taskDao()
    @Provides fun provideLocationDao(db: Family4Database): LocationDao = db.locationDao()
    @Provides fun provideHealthDao(db: Family4Database): HealthDao = db.healthDao()
    @Provides fun provideAlbumDao(db: Family4Database): AlbumDao = db.albumDao()
}
