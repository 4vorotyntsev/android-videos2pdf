package com.vs.videoscanpdf.ui.moments

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.session.DetectedMoment
import com.vs.videoscanpdf.data.session.ExcludedMoment
import com.vs.videoscanpdf.data.session.ExclusionReason
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * UI state for auto moments screen.
 */
data class AutoMomentsUiState(
    val isAnalyzing: Boolean = true,
    val analysisProgress: Float = 0f,
    val detectedMoments: List<DetectedMoment> = emptyList(),
    val excludedMoments: List<ExcludedMoment> = emptyList(),
    val selectedMomentIds: Set<String> = emptySet(),
    val showAdvancedSettings: Boolean = false,
    val extractionDensity: Float = 0.5f,
    val error: String? = null
) {
    val selectedCount: Int
        get() = selectedMomentIds.size
    
    val excludedCount: Int
        get() = excludedMoments.size
}

@HiltViewModel
class AutoMomentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private var sessionId: String? = null
    
    private val _uiState = MutableStateFlow(AutoMomentsUiState())
    val uiState: StateFlow<AutoMomentsUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.AUTO_MOMENTS)
        
        // Start page detection
        runPageDetection()
    }
    
    private fun runPageDetection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzing = true,
                analysisProgress = 0f
            )
            
            try {
                val session = sessionManager.currentSession ?: return@launch
                val videoUri = session.sourceVideoUri ?: return@launch
                val trimStart = session.trimRange.startMs
                val trimEnd = if (session.trimRange.endMs == Long.MAX_VALUE) {
                    session.videoDurationMs
                } else {
                    session.trimRange.endMs
                }
                
                val result = withContext(Dispatchers.IO) {
                    detectPages(
                        videoUri = videoUri,
                        startMs = trimStart,
                        endMs = trimEnd,
                        density = _uiState.value.extractionDensity
                    ) { progress ->
                        _uiState.value = _uiState.value.copy(analysisProgress = progress)
                    }
                }
                
                val (moments, excluded) = result
                val selectedIds = moments.filter { it.isSelected }.map { it.id }.toSet()
                
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    detectedMoments = moments,
                    excludedMoments = excluded,
                    selectedMomentIds = selectedIds,
                    analysisProgress = 1f
                )
                
                // Update session with detected moments
                sessionManager.setDetectedMoments(moments, excluded)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzing = false,
                    error = "Detection failed: ${e.message}"
                )
            }
        }
    }
    
    private fun detectPages(
        videoUri: String,
        startMs: Long,
        endMs: Long,
        density: Float,
        onProgress: (Float) -> Unit
    ): Pair<List<DetectedMoment>, List<ExcludedMoment>> {
        val moments = mutableListOf<DetectedMoment>()
        val excluded = mutableListOf<ExcludedMoment>()
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(videoUri))
            
            // Calculate sample interval based on density
            // Higher density = more samples
            val durationMs = endMs - startMs
            val baseSamples = 10
            val maxSamples = 30
            val sampleCount = (baseSamples + (density * (maxSamples - baseSamples))).toInt()
            val intervalMs = durationMs / sampleCount
            
            var lastGoodFrame: Bitmap? = null
            var frameCount = 0
            
            for (i in 0 until sampleCount) {
                val timeMs = startMs + (i * intervalMs)
                val timeUs = timeMs * 1000
                
                onProgress(i.toFloat() / sampleCount)
                
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: continue
                
                // Simple quality check (in real app, use CV for better detection)
                val quality = estimateFrameQuality(frame)
                
                if (quality > 0.5f) {
                    // Check for duplicate (compare with last good frame)
                    val isDuplicate = lastGoodFrame?.let { isSimilar(it, frame) } ?: false
                    
                    if (isDuplicate) {
                        excluded.add(
                            ExcludedMoment(
                                timeMs = timeMs,
                                reason = ExclusionReason.DUPLICATE,
                                thumbnail = Bitmap.createScaledBitmap(frame, 120, 160, true)
                            )
                        )
                    } else {
                        val thumbnail = Bitmap.createScaledBitmap(frame, 120, 160, true)
                        moments.add(
                            DetectedMoment(
                                id = UUID.randomUUID().toString(),
                                timeMs = timeMs,
                                thumbnail = thumbnail,
                                isSelected = true,
                                qualityScore = quality
                            )
                        )
                        lastGoodFrame = frame
                        frameCount++
                    }
                } else {
                    // Exclude low quality
                    val reason = when {
                        quality < 0.3f -> ExclusionReason.BLUR
                        else -> ExclusionReason.MOTION_BLUR
                    }
                    excluded.add(
                        ExcludedMoment(
                            timeMs = timeMs,
                            reason = reason,
                            thumbnail = Bitmap.createScaledBitmap(frame, 120, 160, true)
                        )
                    )
                }
            }
            
            onProgress(1f)
            
        } finally {
            retriever.release()
        }
        
        return Pair(moments, excluded)
    }
    
    private fun estimateFrameQuality(bitmap: Bitmap): Float {
        // Simple quality estimation based on variance (blur detection)
        // In real app, use OpenCV or ML for better quality assessment
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Calculate luminance variance as proxy for sharpness
        var sum = 0.0
        var sumSq = 0.0
        var count = 0
        
        // Sample every 10th pixel for speed
        for (i in pixels.indices step 10) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val luminance = 0.299 * r + 0.587 * g + 0.114 * b
            sum += luminance
            sumSq += luminance * luminance
            count++
        }
        
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        
        // Normalize to 0-1 range (higher variance = sharper image)
        return (variance / 5000.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    private fun isSimilar(bitmap1: Bitmap, bitmap2: Bitmap): Boolean {
        // Simple similarity check using average color difference
        // In real app, use perceptual hashing or feature matching
        val size = 8
        val scaled1 = Bitmap.createScaledBitmap(bitmap1, size, size, true)
        val scaled2 = Bitmap.createScaledBitmap(bitmap2, size, size, true)
        
        var diff = 0
        for (x in 0 until size) {
            for (y in 0 until size) {
                val p1 = scaled1.getPixel(x, y)
                val p2 = scaled2.getPixel(x, y)
                
                val r1 = (p1 shr 16) and 0xFF
                val g1 = (p1 shr 8) and 0xFF
                val b1 = p1 and 0xFF
                
                val r2 = (p2 shr 16) and 0xFF
                val g2 = (p2 shr 8) and 0xFF
                val b2 = p2 and 0xFF
                
                diff += kotlin.math.abs(r1 - r2) + kotlin.math.abs(g1 - g2) + kotlin.math.abs(b1 - b2)
            }
        }
        
        val maxDiff = size * size * 3 * 255
        val similarity = 1.0 - (diff.toDouble() / maxDiff)
        
        return similarity > 0.9 // Consider similar if > 90% match
    }
    
    /**
     * Toggle selection of a moment.
     */
    fun toggleMomentSelection(momentId: String) {
        val currentIds = _uiState.value.selectedMomentIds
        val newIds = if (momentId in currentIds) {
            currentIds - momentId
        } else {
            currentIds + momentId
        }
        _uiState.value = _uiState.value.copy(selectedMomentIds = newIds)
        
        sessionManager.toggleMomentSelection(momentId)
    }
    
    /**
     * Toggle advanced settings visibility.
     */
    fun toggleAdvancedSettings() {
        _uiState.value = _uiState.value.copy(
            showAdvancedSettings = !_uiState.value.showAdvancedSettings
        )
    }
    
    /**
     * Set extraction density.
     */
    fun setExtractionDensity(density: Float) {
        _uiState.value = _uiState.value.copy(extractionDensity = density)
    }
    
    /**
     * Re-run detection with current settings.
     */
    fun rerunDetection() {
        runPageDetection()
    }
    
    /**
     * Confirm selection and prepare for processing.
     */
    fun confirmSelection() {
        val selectedIds = _uiState.value.selectedMomentIds
        val selectedMoments = _uiState.value.detectedMoments.filter { it.id in selectedIds }
        sessionManager.setPageOrder(selectedMoments.map { it.id })
    }
}

