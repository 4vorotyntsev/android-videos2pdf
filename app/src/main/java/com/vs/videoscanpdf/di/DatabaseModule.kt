package com.vs.videoscanpdf.di

import android.content.Context
import androidx.room.Room
import com.vs.videoscanpdf.data.dao.ExportDao
import com.vs.videoscanpdf.data.dao.PageDao
import com.vs.videoscanpdf.data.dao.ProjectDao
import com.vs.videoscanpdf.data.dao.TelemetryDao
import com.vs.videoscanpdf.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database and DAO dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()
    
    @Provides
    fun providePageDao(database: AppDatabase): PageDao = database.pageDao()
    
    @Provides
    fun provideExportDao(database: AppDatabase): ExportDao = database.exportDao()
    
    @Provides
    fun provideTelemetryDao(database: AppDatabase): TelemetryDao = database.telemetryDao()
}
