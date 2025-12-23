package com.vs.videoscanpdf.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vs.videoscanpdf.data.entities.ExportEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for exports.
 */
@Dao
interface ExportDao {
    
    @Query("SELECT * FROM exports ORDER BY createdAt DESC")
    fun getAllExports(): Flow<List<ExportEntity>>
    
    @Query("SELECT * FROM exports WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getExportsByProject(projectId: String): Flow<List<ExportEntity>>
    
    @Query("SELECT * FROM exports ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentExports(limit: Int = 10): List<ExportEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(export: ExportEntity)
    
    @Query("DELETE FROM exports WHERE id = :exportId")
    suspend fun deleteById(exportId: String)
    
    @Query("DELETE FROM exports WHERE projectId = :projectId")
    suspend fun deleteAllByProject(projectId: String)
    
    @Query("UPDATE exports SET pdfPath = :newPath WHERE id = :exportId")
    suspend fun updatePath(exportId: String, newPath: String)
}
