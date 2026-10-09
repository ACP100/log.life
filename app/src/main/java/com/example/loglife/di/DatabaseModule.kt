package com.example.loglife.di

import android.content.Context
import com.example.loglife.data.LogLifeDatabase
import com.example.loglife.data.dao.EventInstanceDao
import com.example.loglife.data.dao.EventTypeDao
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
    fun provideEventTypeDao(database: LogLifeDatabase): EventTypeDao {
        return database.eventTypeDao()
    }

    @Provides
    @Singleton
    fun provideEventInstanceDao(database: LogLifeDatabase): EventInstanceDao {
        return database.eventInstanceDao()
    }
}