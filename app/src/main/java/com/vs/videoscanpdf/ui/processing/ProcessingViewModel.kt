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
                val totalPages = session.selectedMomentIds.size
                
                _uiState.value = _uiState.value.copy(totalPages = totalPages)
                
                // Stage 1: Reading video
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.READING_VIDEO,
                    overallProgress = 0.1f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.READING_VIDEO, 0.1f)
                
                delay(500) // Simulated work
                if (isCancelled) return@launch
                
                // Stage 2: Detecting pages
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.DETECTING_PAGES,
                    overallProgress = 0.25f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.DETECTING_PAGES, 0.25f)
                
                delay(500)
                if (isCancelled) return@launch
                
                // Stage 3: Enhancing images
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.ENHANCING_IMAGES,
                    overallProgress = 0.4f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.ENHANCING_IMAGES, 0.4f)
                
                // Process each page
                val selectedMoments = session.detectedMoments.filter { it.id in session.selectedMomentIds }
                
                selectedMoments.forEachIndexed { index, moment ->
                    if (isCancelled) return@launch
                    
                    val progress = 0.4f + (0.5f * (index + 1) / totalPages)
                    _uiState.value = _uiState.value.copy(
                        overallProgress = progress,
                        pagesProcessed = index + 1,
                        previewBitmap = moment.thumbnail
                    )
                    sessionManager.updateProcessingProgress(ProcessingStage.ENHANCING_IMAGES, progress)
                    
                    // Simulated enhancement
                    delay(200)
                }
                
                if (isCancelled) return@launch
                
                // Stage 4: Generating PDF
                _uiState.value = _uiState.value.copy(
                    currentStage = ProcessingStage.GENERATING_PDF,
                    overallProgress = 0.95f
                )
                sessionManager.updateProcessingProgress(ProcessingStage.GENERATING_PDF, 0.95f)
                
                delay(500)
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
