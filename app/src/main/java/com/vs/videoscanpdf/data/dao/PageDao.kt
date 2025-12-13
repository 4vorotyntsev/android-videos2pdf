package com.vs.videoscanpdf.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vs.videoscanpdf.data.entities.PageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for pages.
 */
@Dao
interface PageDao {
    
    @Query("SELECT * FROM pages WHERE projectId = :projectId ORDER BY `index` ASC")
    fun getPagesByProject(projectId: String): Flow<List<PageEntity>>
    
    @Query("SELECT * FROM pages WHERE projectId = :projectId ORDER BY `index` ASC")
    suspend fun getPagesByProjectSync(projectId: String): List<PageEntity>
    
    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getPageById(pageId: String): PageEntity?
    
    @Query("SELECT COUNT(*) FROM pages WHERE projectId = :projectId")
    suspend fun getPageCount(projectId: String): Int
    
    @Query("SELECT COUNT(*) FROM pages WHERE projectId = :projectId")
    fun getPageCountFlow(projectId: String): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<PageEntity>)
    
    @Update
    suspend fun update(page: PageEntity)
    
    @Delete
    suspend fun delete(page: PageEntity)
    
    @Query("DELETE FROM pages WHERE id = :pageId")
    suspend fun deleteById(pageId: String)
    
    @Query("DELETE FROM pages WHERE projectId = :projectId")
    suspend fun deleteAllByProject(projectId: String)
    
    @Query("UPDATE pages SET `index` = :newIndex WHERE id = :pageId")
    suspend fun updateIndex(pageId: String, newIndex: Int)
    
    @Query("UPDATE pages SET rotation = :rotation WHERE id = :pageId")
    suspend fun updateRotation(pageId: String, rotation: Int)
}
