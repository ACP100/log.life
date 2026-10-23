package com.example.loglife.di

import android.content.Context
import com.example.loglife.data.LogLifeDatabase
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventTypeDao
import com.example.loglife.data.dao.NoteDao
import com.example.loglife.data.dao.PlanDao
import com.example.loglife.data.settings.AppSettingsRepository
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
    fun provideDatabase(@ApplicationContext context: Context): LogLifeDatabase {
        return LogLifeDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAppSettings(@ApplicationContext context: Context): AppSettingsRepository =
        AppSettingsRepository.get(context)

    @Provides
    @Singleton
    fun provideEventTypeDao(database: LogLifeDatabase): EventTypeDao {
        return database.eventTypeDao()
    }

    @Provides
    @Singleton
    fun provideEventInstanceDao(database: LogLifeDatabase): EventInstanceDao {
        return database.eventInstanceDao()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: LogLifeDatabase): NoteDao = database.noteDao()

    @Provides
    @Singleton
    fun providePlanDao(database: LogLifeDatabase): PlanDao = database.planDao()
}
