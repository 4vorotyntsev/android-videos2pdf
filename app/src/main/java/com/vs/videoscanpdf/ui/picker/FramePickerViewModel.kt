package com.vs.videoscanpdf.ui.picker

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.PageEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Frame Picker screen.
 * Handles video playback state and frame extraction.
 */
@HiltViewModel
class FramePickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FramePickerUiState())
    val uiState: StateFlow<FramePickerUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    private var mediaRetriever: MediaMetadataRetriever? = null
    
    fun initialize(projectId: String, importedVideoUri: String? = null) {
        currentProjectId = projectId
        if (importedVideoUri != null) {
            loadImportedVideo(importedVideoUri)
        } else {
            loadProject()
        }
    }
    
    private fun loadImportedVideo(videoUri: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Copy imported video to project directory
                val videoPath = withContext(Dispatchers.IO) {
                    val targetFile = projectRepository.getVideoPath(currentProjectId)
                    targetFile.parentFile?.mkdirs()
                    
                    // Copy from content URI to local file
                    val uri = android.net.Uri.parse(videoUri)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    targetFile.absolutePath
                }
                
                // Get duration using MediaMetadataRetriever
                val duration = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(videoPath)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    } finally {
                        retriever.release()
                    }
                }
                
                // Update project with video path
                projectRepository.setVideoPath(currentProjectId, videoPath, duration)
                
                _uiState.value = _uiState.value.copy(
                    videoPath = videoPath,
                    videoDurationMs = duration,
                    isLoading = false
                )
                
                initializeMediaRetriever(videoPath)
                loadThumbnails()
                loadExistingPages()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to import video: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadProject() {
        viewModelScope.launch {
            try {
                val project = projectRepository.getProjectById(currentProjectId)
                if (project?.videoPath != null) {
                    _uiState.value = _uiState.value.copy(
                        videoPath = project.videoPath,
                        videoDurationMs = project.videoDurationMs,
                        isLoading = false
                    )
                    initializeMediaRetriever(project.videoPath)
                    loadThumbnails()
                    loadExistingPages()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "No video found for this project",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    private fun loadExistingPages() {
        viewModelScope.launch {
            projectRepository.getPagesByProject(currentProjectId).collect { pages ->
                val selectedPages = pages.map { page ->
                    SelectedPage(
                        id = page.id,
                        timeMs = page.sourceTimeMs,
                        thumbnail = extractFrame(page.sourceTimeMs)
                    )
                }
                _uiState.value = _uiState.value.copy(selectedPages = selectedPages)
            }
        }
    }
    
    private suspend fun initializeMediaRetriever(videoPath: String) = withContext(Dispatchers.IO) {
        try {
            mediaRetriever = MediaMetadataRetriever().apply {
                setDataSource(videoPath)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to load video: ${e.message}")
        }
    }
    
    private fun loadThumbnails() {
        viewModelScope.launch(Dispatchers.IO) {
            val duration = _uiState.value.videoDurationMs
            if (duration <= 0) return@launch
            
            val thumbnails = mutableListOf<Thumbnail>()
            val interval = (duration / 10).coerceAtLeast(1000L) // At least 1 second apart
            
            var time = 0L
            while (time < duration) {
                val bitmap = extractFrame(time)
                if (bitmap != null) {
                    thumbnails.add(Thumbnail(time, bitmap))
                }
                time += interval
                
                // Update progressively
                _uiState.value = _uiState.value.copy(thumbnails = thumbnails.toList())
            }
            
            // Auto-detect pages after thumbnails are loaded
            autoDetectPages()
        }
    }
    
    /**
     * Auto-detect page moments from the video.
     * Since CV is skipped, this uses a simple interval-based approach.
     */
    private suspend fun autoDetectPages() = withContext(Dispatchers.IO) {
        val duration = _uiState.value.videoDurationMs
        if (duration <= 0) return@withContext
        
        _uiState.value = _uiState.value.copy(isAutoDetecting = true, autoDetectionProgress = 0f)
        
        val density = _uiState.value.extractionDensity
        // Calculate interval based on density: more density = more pages
        val baseInterval = 3000L // 3 seconds base
        val interval = (baseInterval * (1.5f - density)).toLong().coerceAtLeast(1500L)
        
        val detectedPages = mutableListOf<DetectedPage>()
        var time = interval / 2 // Start from middle of first interval
        var progress = 0f
        
        while (time < duration) {
            val bitmap = extractFrame(time)
            if (bitmap != null) {
                detectedPages.add(
                    DetectedPage(
                        timeMs = time,
                        thumbnail = bitmap,
                        isSelected = true,
                        quality = PageQuality.GOOD
                    )
                )
            }
            time += interval
            progress = (time.toFloat() / duration).coerceAtMost(1f)
            _uiState.value = _uiState.value.copy(
                autoDetectionProgress = progress,
                detectedPages = detectedPages.toList()
            )
        }
        
        _uiState.value = _uiState.value.copy(
            isAutoDetecting = false,
            autoDetectionProgress = 1f,
            detectedPages = detectedPages.toList()
        )
    }
    
    fun toggleAdvancedSettings() {
        _uiState.value = _uiState.value.copy(
            showAdvancedSettings = !_uiState.value.showAdvancedSettings
        )
    }
    
    fun setExtractionDensity(density: Float) {
        _uiState.value = _uiState.value.copy(extractionDensity = density)
    }
    
    fun rerunAutoDetection() {
        viewModelScope.launch(Dispatchers.IO) {
            autoDetectPages()
        }
    }
    
    fun toggleDetectedPageSelection(timeMs: Long) {
        val updatedPages = _uiState.value.detectedPages.map { page ->
            if (page.timeMs == timeMs) {
                page.copy(isSelected = !page.isSelected)
            } else {
                page
            }
        }
        _uiState.value = _uiState.value.copy(detectedPages = updatedPages)
    }
    
    /**
     * Add all selected detected pages to the project.
     */
    fun confirmDetectedPages() {
        viewModelScope.launch {
            val selectedDetected = _uiState.value.detectedPages.filter { it.isSelected }
            
            selectedDetected.forEachIndexed { index, detected ->
                val pageId = UUID.randomUUID().toString()
                val bitmap = detected.thumbnail ?: extractFrame(detected.timeMs) ?: return@forEachIndexed
                
                val imagePath = saveFrameToFile(pageId, bitmap)
                
                val page = PageEntity(
                    id = pageId,
                    projectId = currentProjectId,
                    index = index,
                    sourceTimeMs = detected.timeMs,
                    imagePath = imagePath
                )
                
                projectRepository.addPage(page)
            }
        }
    }
    
    private suspend fun extractFrame(timeMs: Long): Bitmap? = withContext(Dispatchers.IO) {
        try {
            mediaRetriever?.getFrameAtTime(
                timeMs * 1000, // Convert to microseconds
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun seekTo(timeMs: Long) {
        _uiState.value = _uiState.value.copy(currentPositionMs = timeMs)
        viewModelScope.launch {
            val frame = extractFrame(timeMs)
            _uiState.value = _uiState.value.copy(currentFrame = frame)
        }
    }
    
    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }
    
    fun addCurrentFrame() {
        viewModelScope.launch {
            val currentTime = _uiState.value.currentPositionMs
            val frame = extractFrame(currentTime) ?: return@launch
            
            // Generate page ID
            val pageId = UUID.randomUUID().toString()
            
            // Save frame to file
            val imagePath = saveFrameToFile(pageId, frame)
            
            // Get next index
            val currentPages = _uiState.value.selectedPages
            val nextIndex = currentPages.size
            
            // Create page entity
            val page = PageEntity(
                id = pageId,
                projectId = currentProjectId,
                index = nextIndex,
                sourceTimeMs = currentTime,
                imagePath = imagePath
            )
            
            projectRepository.addPage(page)
            
            // Update UI immediately
            val newPage = SelectedPage(
                id = pageId,
                timeMs = currentTime,
                thumbnail = frame
            )
            _uiState.value = _uiState.value.copy(
                selectedPages = currentPages + newPage
            )
        }
    }
    
    fun removePage(pageId: String) {
        viewModelScope.launch {
            projectRepository.deletePage(pageId, currentProjectId)
            val updatedPages = _uiState.value.selectedPages.filter { it.id != pageId }
            _uiState.value = _uiState.value.copy(selectedPages = updatedPages)
        }
    }
    
    private suspend fun saveFrameToFile(pageId: String, bitmap: Bitmap): String = 
        withContext(Dispatchers.IO) {
            val file = projectRepository.getPageImagePath(currentProjectId, pageId)
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            
            file.absolutePath
        }
    
    fun canContinue(): Boolean = _uiState.value.selectedPages.isNotEmpty()
    
    override fun onCleared() {
        super.onCleared()
        mediaRetriever?.release()
    }
}
