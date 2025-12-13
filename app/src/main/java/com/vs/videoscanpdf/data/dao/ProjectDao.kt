package com.vs.videoscanpdf.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vs.videoscanpdf.data.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for projects.
 */
@Dao
interface ProjectDao {
    
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)
    
    @Update
    suspend fun update(project: ProjectEntity)
    
    @Delete
    suspend fun delete(project: ProjectEntity)
    
    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: String)
    
    @Query("UPDATE projects SET updatedAt = :timestamp WHERE id = :projectId")
    suspend fun updateTimestamp(projectId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE projects SET isVideoDeleted = 1, videoPath = NULL WHERE id = :projectId")
    suspend fun markVideoDeleted(projectId: String)
}
