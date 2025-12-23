package com.vs.videoscanpdf.ui.processing

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProcessingUiState(
    val currentStep: ProcessingStep = ProcessingStep.READING_VIDEO,
    val progress: Float = 0f,
    val overallProgress: Float = 0f,
    val previewBitmap: Bitmap? = null,
    val processedPages: Int = 0,
    val totalPages: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProcessingUiState())
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()
    
    private var processingJob: Job? = null
    private var isRunningInBackground = false
    
    fun startProcessing(projectId: String) {
        processingJob = viewModelScope.launch {
            try {
                val project = projectRepository.getProjectById(projectId)
                val videoPath = project?.videoPath ?: return@launch
                
                // Step 1: Reading video
                _uiState.value = _uiState.value.copy(
                    currentStep = ProcessingStep.READING_VIDEO,
                    progress = 0f
                )
                
                val pages = projectRepository.getPagesByProjectSync(projectId)
                _uiState.value = _uiState.value.copy(
                    totalPages = pages.size.coerceAtLeast(1)
                )
                
                // Simulate reading video
                for (i in 1..10) {
                    if (!isActive) return@launch
                    delay(100)
                    _uiState.value = _uiState.value.copy(
                        progress = i / 10f,
                        overallProgress = (i / 10f) * 0.25f
                    )
                }
                
                // Step 2: Detecting pages
                _uiState.value = _uiState.value.copy(
                    currentStep = ProcessingStep.DETECTING_PAGES,
                    progress = 0f
                )
                
                for (i in 1..10) {
                    if (!isActive) return@launch
                    delay(100)
                    _uiState.value = _uiState.value.copy(
                        progress = i / 10f,
                        overallProgress = 0.25f + (i / 10f) * 0.25f
                    )
                }
                
                // Step 3: Enhancing images
                _uiState.value = _uiState.value.copy(
                    currentStep = ProcessingStep.ENHANCING_IMAGES,
                    progress = 0f
                )
                
                // Load first page preview
                withContext(Dispatchers.IO) {
                    pages.firstOrNull()?.let { page ->
                        val bitmap = android.graphics.BitmapFactory.decodeFile(page.imagePath)
                        _uiState.value = _uiState.value.copy(previewBitmap = bitmap)
                    }
                }
                
                val totalPagesCount = pages.size.coerceAtLeast(1)
                for (i in 1..totalPagesCount) {
                    if (!isActive) return@launch
                    delay(200)
                    _uiState.value = _uiState.value.copy(
                        progress = i.toFloat() / totalPagesCount,
                        overallProgress = 0.5f + (i.toFloat() / totalPagesCount) * 0.25f,
                        processedPages = i
                    )
                }
                
                // Step 4: Generating PDF
                _uiState.value = _uiState.value.copy(
                    currentStep = ProcessingStep.GENERATING_PDF,
                    progress = 0f
                )
                
                for (i in 1..10) {
                    if (!isActive) return@launch
                    delay(100)
                    _uiState.value = _uiState.value.copy(
                        progress = i / 10f,
                        overallProgress = 0.75f + (i / 10f) * 0.25f
                    )
                }
                
                // Complete
                _uiState.value = _uiState.value.copy(
                    currentStep = ProcessingStep.COMPLETE,
                    progress = 1f,
                    overallProgress = 1f,
                    isComplete = true
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }
    
    fun runInBackground() {
        isRunningInBackground = true
        // Processing continues in background
    }
    
    fun cancelProcessing() {
        processingJob?.cancel()
    }
    
    override fun onCleared() {
        super.onCleared()
        if (!isRunningInBackground) {
            processingJob?.cancel()
        }
    }
}

