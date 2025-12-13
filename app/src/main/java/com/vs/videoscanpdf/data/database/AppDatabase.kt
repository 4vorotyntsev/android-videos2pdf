package com.vs.videoscanpdf.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vs.videoscanpdf.data.dao.ExportDao
import com.vs.videoscanpdf.data.dao.PageDao
import com.vs.videoscanpdf.data.dao.ProjectDao
import com.vs.videoscanpdf.data.dao.TelemetryDao
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.entities.PageEntity
import com.vs.videoscanpdf.data.entities.ProjectEntity
import com.vs.videoscanpdf.data.entities.TelemetryEventEntity

/**
 * Room database for VideoScan PDF.
 * Contains all entities for projects, pages, exports, and telemetry.
 */
@Database(
    entities = [
        ProjectEntity::class,
        PageEntity::class,
        ExportEntity::class,
        TelemetryEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun pageDao(): PageDao
    abstract fun exportDao(): ExportDao
    abstract fun telemetryDao(): TelemetryDao
    
    companion object {
        const val DATABASE_NAME = "videoscanpdf_db"
    }
}
