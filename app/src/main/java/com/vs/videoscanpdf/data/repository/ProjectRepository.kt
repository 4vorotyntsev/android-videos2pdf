package com.vs.videoscanpdf.data.repository

import android.content.Context
import com.vs.videoscanpdf.data.dao.ExportDao
import com.vs.videoscanpdf.data.dao.PageDao
import com.vs.videoscanpdf.data.dao.ProjectDao
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.entities.PageEntity
import com.vs.videoscanpdf.data.entities.ProjectEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing projects and their associated files.
 * Handles both database operations and file system management.
 */
@Singleton
class ProjectRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectDao: ProjectDao,
    private val pageDao: PageDao,
    private val exportDao: ExportDao
) {
    
    private val projectsDir: File
        get() = File(context.filesDir, "projects").also { it.mkdirs() }
    
    // ============== Projects ==============
    
    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    
    suspend fun getProjectById(projectId: String): ProjectEntity? = projectDao.getProjectById(projectId)
    
    fun getProjectByIdFlow(projectId: String): Flow<ProjectEntity?> = projectDao.getProjectByIdFlow(projectId)
    
    suspend fun createProject(title: String): ProjectEntity {
        val project = ProjectEntity(title = title)
        projectDao.insert(project)
        getProjectDirectory(project.id).mkdirs()
        return project
    }
    
    suspend fun updateProject(project: ProjectEntity) {
        projectDao.update(project.copy(updatedAt = System.currentTimeMillis()))
    }
    
    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        // Delete files first
        getProjectDirectory(projectId).deleteRecursively()
        // Then delete from database (cascade will handle pages and exports)
        projectDao.deleteById(projectId)
    }
    
    suspend fun updateProjectTimestamp(projectId: String) {
        projectDao.updateTimestamp(projectId)
    }
    
    // ============== Pages ==============
    
    fun getPagesByProject(projectId: String): Flow<List<PageEntity>> = pageDao.getPagesByProject(projectId)
    
    suspend fun getPagesByProjectSync(projectId: String): List<PageEntity> = pageDao.getPagesByProjectSync(projectId)
    
    suspend fun getPageCount(projectId: String): Int = pageDao.getPageCount(projectId)
    
    fun getPageCountFlow(projectId: String): Flow<Int> = pageDao.getPageCountFlow(projectId)
    
    suspend fun addPage(page: PageEntity) {
        pageDao.insert(page)
        projectDao.updateTimestamp(page.projectId)
    }
    
    suspend fun updatePage(page: PageEntity) {
        pageDao.update(page)
        projectDao.updateTimestamp(page.projectId)
    }
    
    suspend fun deletePage(pageId: String, projectId: String) = withContext(Dispatchers.IO) {
        val page = pageDao.getPageById(pageId) ?: return@withContext
        // Delete image file
        File(page.imagePath).delete()
        // Delete from database
        pageDao.deleteById(pageId)
        projectDao.updateTimestamp(projectId)
    }
    
    suspend fun reorderPages(pages: List<PageEntity>) {
        pages.forEachIndexed { index, page ->
            pageDao.updateIndex(page.id, index)
        }
        pages.firstOrNull()?.let { projectDao.updateTimestamp(it.projectId) }
    }
    
    suspend fun rotatePage(pageId: String, rotation: Int) {
        pageDao.updateRotation(pageId, rotation)
    }
    
    // ============== Exports ==============
    
    fun getAllExports(): Flow<List<ExportEntity>> = exportDao.getAllExports()
    
    fun getExportsByProject(projectId: String): Flow<List<ExportEntity>> = 
        exportDao.getExportsByProject(projectId)
    
    suspend fun addExport(export: ExportEntity) {
        exportDao.insert(export)
        projectDao.updateTimestamp(export.projectId)
    }
    
    // ============== Video Management ==============
    
    suspend fun setVideoPath(projectId: String, videoPath: String, durationMs: Long) {
        val project = projectDao.getProjectById(projectId) ?: return
        projectDao.update(project.copy(
            videoPath = videoPath,
            videoDurationMs = durationMs,
            updatedAt = System.currentTimeMillis()
        ))
    }
    
    suspend fun deleteVideo(projectId: String) = withContext(Dispatchers.IO) {
        val project = projectDao.getProjectById(projectId) ?: return@withContext
        project.videoPath?.let { File(it).delete() }
        projectDao.markVideoDeleted(projectId)
    }
    
    // ============== File Paths ==============
    
    fun getProjectDirectory(projectId: String): File = 
        File(projectsDir, projectId).also { it.mkdirs() }
    
    fun getVideoPath(projectId: String): File = 
        File(getProjectDirectory(projectId), "video.mp4")
    
    fun getPagesDirectory(projectId: String): File = 
        File(getProjectDirectory(projectId), "pages").also { it.mkdirs() }
    
    fun getExportsDirectory(projectId: String): File = 
        File(getProjectDirectory(projectId), "exports").also { it.mkdirs() }
    
    fun getPageImagePath(projectId: String, pageId: String): File = 
        File(getPagesDirectory(projectId), "$pageId.jpg")
    
    fun getExportPdfPath(projectId: String, exportId: String): File = 
        File(getExportsDirectory(projectId), "$exportId.pdf")
}
