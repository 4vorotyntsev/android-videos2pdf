package com.vs.videoscanpdf.ui.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.StatFs
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
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Stability state for user feedback.
 */
enum class StabilityState {
    STABLE,
    SLIGHTLY_SHAKY,
    TOO_SHAKY
}

/**
 * ViewModel for the Recorder screen.
 * Manages CameraX setup, video recording, and stability detection.
 */
@HiltViewModel
class RecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val sessionManager: SessionManager
) : ViewModel(), SensorEventListener {
    
    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()
    
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: androidx.camera.core.Camera? = null
    private var recordingStartTime: Long = 0L
    
    private var sessionId: String? = null
    private lateinit var outputFile: File
    
    // Stability detection
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastAcceleration = 0f
    private val accelerationHistory = mutableListOf<Float>()
    
    companion object {
        private const val MIN_FREE_SPACE_MB = 100L // Minimum 100MB free space
        private const val STABILITY_THRESHOLD_STABLE = 1.5f
        private const val STABILITY_THRESHOLD_SHAKY = 3.0f
    }
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.RECORDING)
        
        // Check available storage
        checkStorage()
        
        // Start stability monitoring
        startStabilityMonitoring()
    }
    
    private fun checkStorage() {
        val stat = StatFs(context.cacheDir.path)
        val availableMB = stat.availableBytes / (1024 * 1024)
        
        if (availableMB < MIN_FREE_SPACE_MB) {
            _uiState.value = _uiState.value.copy(
                hasLowStorage = true,
                error = "Low storage space. Please free up some space before recording."
            )
        }
    }
    
    private fun startStabilityMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { e ->
            if (e.sensor.type == Sensor.TYPE_ACCELEROMETER && _uiState.value.isRecording) {
                val x = e.values[0]
                val y = e.values[1]
                val z = e.values[2]
                
                val acceleration = sqrt(x * x + y * y + z * z)
                val delta = kotlin.math.abs(acceleration - lastAcceleration)
                lastAcceleration = acceleration
                
                accelerationHistory.add(delta)
                if (accelerationHistory.size > 10) {
                    accelerationHistory.removeAt(0)
                }
                
                val avgDelta = accelerationHistory.average().toFloat()
                val stability = when {
                    avgDelta < STABILITY_THRESHOLD_STABLE -> StabilityState.STABLE
                    avgDelta < STABILITY_THRESHOLD_SHAKY -> StabilityState.SLIGHTLY_SHAKY
                    else -> StabilityState.TOO_SHAKY
                }
                
                _uiState.value = _uiState.value.copy(stabilityState = stability)
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
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
        if (_uiState.value.hasLowStorage) {
            _uiState.value = _uiState.value.copy(error = "Not enough storage space")
            return
        }
        
        val videoCapture = videoCapture ?: return
        
        // Prepare output file in session temp directory
        val session = sessionManager.currentSession
        val tempDir = session?.tempDir ?: File(context.cacheDir, "temp_recording")
        tempDir.mkdirs()
        
        outputFile = File(tempDir, "recording_${System.currentTimeMillis()}.mp4")
        
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        
        accelerationHistory.clear()
        lastAcceleration = SensorManager.GRAVITY_EARTH
        
        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .start(ContextCompat.getMainExecutor(context)) { event ->
                handleRecordingEvent(event)
            }
        
        recordingStartTime = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            isRecording = true,
            recordingDurationMs = 0L,
            stabilityState = StabilityState.STABLE
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
                    // Update session with video info
                    sessionManager.setSourceVideo(
                        uri = outputFile.absolutePath,
                        durationMs = durationMs,
                        isImported = false
                    )
                    sessionManager.setStatus(SessionStatus.REVIEW_VIDEO)
                    
                    // Show Use/Retake buttons
                    _uiState.value = _uiState.value.copy(
                        showReviewButtons = true,
                        recordedVideoPath = outputFile.absolutePath,
                        recordingDurationMs = durationMs
                    )
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
    
    /**
     * User confirmed to use the recorded video.
     */
    fun useVideo() {
        _uiState.value = _uiState.value.copy(recordingComplete = true)
    }
    
    /**
     * User wants to retake the video.
     */
    fun retake() {
        // Delete the recorded video
        _uiState.value.recordedVideoPath?.let { path ->
            File(path).delete()
        }
        
        sessionManager.setStatus(SessionStatus.RECORDING)
        
        // Reset state
        _uiState.value = _uiState.value.copy(
            showReviewButtons = false,
            recordedVideoPath = null
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
        cameraProvider?.unbindAll()
        sensorManager.unregisterListener(this)
    }
}
