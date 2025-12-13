package com.vs.videoscanpdf.ui.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject

/**
 * ViewModel for the Recorder screen.
 * Manages CameraX setup and video recording.
 */
@HiltViewModel
class RecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()
    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: androidx.camera.core.Camera? = null
    private var recordingStartTime: Long = 0L
    
    private lateinit var currentProjectId: String
    private lateinit var outputFile: File
    
    fun initialize(projectId: String) {
        currentProjectId = projectId
    }
    
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun setupCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        executor: Executor
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(previewView, lifecycleOwner)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to initialize camera: ${e.message}")
            }
        }, executor)
    }
    
    private fun bindCameraUseCases(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProvider = cameraProvider ?: return
        
        // Preview use case
        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = previewView.surfaceProvider
            }
        
        // Video capture use case with quality selector
        val qualitySelector = QualitySelector.from(
            Quality.FHD,  // 1080p default
            androidx.camera.video.FallbackStrategy.higherQualityOrLowerThan(Quality.FHD)
        )
        
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()
        
        videoCapture = VideoCapture.withOutput(recorder)
        
        // Select back camera
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to bind camera: ${e.message}")
        }
    }
    
    fun startRecording() {
        val videoCapture = videoCapture ?: return
        
        // Prepare output file
        outputFile = projectRepository.getVideoPath(currentProjectId)
        outputFile.parentFile?.mkdirs()
        
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                handleRecordingEvent(event)
            }
        
        recordingStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDurationMs = 0L
        )
        
        // Start duration timer
        startDurationTimer()
    }
    
    private fun startDurationTimer() {
        viewModelScope.launch {
            while (_uiState.value.isRecording) {
                kotlinx.coroutines.delay(100)
                val duration = System.currentTimeMillis() - recordingStartTime
                _uiState.value = _uiState.value.copy(recordingDurationMs = duration)
            }
        }
    }
    
    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }
    
    private fun handleRecordingEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                // Recording started
            }
            is VideoRecordEvent.Finalize -> {
                val durationMs = System.currentTimeMillis() - recordingStartTime
                _uiState.value = _uiState.value.copy(isRecording = false)
                
                if (!event.hasError()) {
                    // Save video info to project
                    viewModelScope.launch {
                        projectRepository.setVideoPath(
                            currentProjectId,
                            outputFile.absolutePath,
                            durationMs
                        )
                        _uiState.value = _uiState.value.copy(recordingComplete = true)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Recording failed: ${event.error}"
                    )
                }
            }
            else -> {}
        }
    }
    
    fun toggleTorch() {
        camera?.let { cam ->
            val newState = !_uiState.value.isTorchEnabled
            cam.cameraControl.enableTorch(newState)
            _uiState.value = _uiState.value.copy(isTorchEnabled = newState)
        }
    }
    
    fun setZoom(level: Float) {
        camera?.cameraControl?.setLinearZoom(level.coerceIn(0f, 1f))
        _uiState.value = _uiState.value.copy(zoomLevel = level)
    }
    
    fun dismissGuidance() {
        _uiState.value = _uiState.value.copy(showGuidance = false)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
        cameraProvider?.unbindAll()
    }
}
