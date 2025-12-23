package com.vs.videoscanpdf.ui.processing

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.session.ProcessingStage
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * UI state for processing screen.
 */
data class ProcessingUiState(
    val currentStage: ProcessingStage = ProcessingStage.IDLE,
    val overallProgress: Float = 0f,
    val previewBitmap: Bitmap? = null,
    val pagesProcessed: Int = 0,
    val totalPages: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Processing screen.
 * 
 * Manual Selection Edition:
 * - No page detection stage (user already picked pages)
 * - Extracts frames at selected timestamps
 * - Enhances images (document filter, perspective correction)
 * - Generates PDF
 */
@HiltViewModel
class ProcessingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private var sessionId: String? = null
    private var isCancelled = false
    
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.PROCESSING)
        isCancelled = false
        
        startProcessing()
    }
    
    private fun startProcessing() {
        viewModelScope.launch {
            try {
                val session = sessionManager.currentSession ?: return@launch
                
                // Manual Selection Edition: Use selectedPages first, fall back to detectedMoments
                val totalPages = if (session.selectedPages.isNotEmpty()) {
                    session.selectedPages.size
                } else {
                    session.selectedMomentIds.size
                }
                
                if (totalPages == 0) {
                    _uiState.value = _uiState.value.copy(error = "No pages selected")
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(totalPages = totalPages)
                
                // Stage 1: Extracting frames from video at selected timestamps
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.EXTRACTING_FRAMES,
                    overallProgress = 0.1f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.EXTRACTING_FRAMES, 0.1f)
                
                // Extract each frame
                val pages = if (session.selectedPages.isNotEmpty()) {
                    session.selectedPages
                } else {
                    session.detectedMoments.filter { it.id in session.selectedMomentIds }
                }
                
                pages.forEachIndexed { index, page ->
                    if (isCancelled) return@launch
                    
                    val progress = 0.1f + (0.2f * (index + 1) / totalPages)
                    val thumbnail = when (page) {
                        is com.vs.videoscanpdf.data.session.SelectedPage -> page.thumbnail
                        is com.vs.videoscanpdf.data.session.DetectedMoment -> page.thumbnail
                        else -> null
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        overallProgress = progress,
                        pagesProcessed = index + 1,
                        previewBitmap = thumbnail
                    )
                    sessionManager.updateProcessingProgress(ProcessingStage.EXTRACTING_FRAMES, progress)
                    
                    // Simulated frame extraction (real implementation would extract at timeMs)
                    delay(100)
                }
                
                if (isCancelled) return@launch
                
                // Stage 2: Enhancing images (document filter, perspective correction)
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.ENHANCING_IMAGES,
                    overallProgress = 0.35f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.ENHANCING_IMAGES, 0.35f)
                
                // Enhance each page
                pages.forEachIndexed { index, page ->
                    if (isCancelled) return@launch
                    
                    val progress = 0.35f + (0.5f * (index + 1) / totalPages)
                    val thumbnail = when (page) {
                        is com.vs.videoscanpdf.data.session.SelectedPage -> page.thumbnail
                        is com.vs.videoscanpdf.data.session.DetectedMoment -> page.thumbnail
                        else -> null
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        overallProgress = progress,
                        pagesProcessed = index + 1,
                        previewBitmap = thumbnail
                    )
                    sessionManager.updateProcessingProgress(ProcessingStage.ENHANCING_IMAGES, progress)
                    
                    // Simulated enhancement (real implementation would apply document filter)
                    delay(150)
                }
                
                if (isCancelled) return@launch
                
                // Stage 3: Generating PDF
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.GENERATING_PDF,
                    overallProgress = 0.9f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.GENERATING_PDF, 0.9f)
                
                delay(500) // Simulated PDF generation
                if (isCancelled) return@launch
                
                // Complete
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.COMPLETE,
                    overallProgress = 1f,
                    isComplete = true
                )
                sessionManager.updateProcessingProgress(ProcessingStage.COMPLETE, 1f)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun runInBackground() {
        // In a real implementation, this would start a foreground service
        // For now, processing continues in the ViewModel
    }
    
    fun cancelProcessing() {
        isCancelled = true
        sessionManager.discardCurrentSession()
    }
}
