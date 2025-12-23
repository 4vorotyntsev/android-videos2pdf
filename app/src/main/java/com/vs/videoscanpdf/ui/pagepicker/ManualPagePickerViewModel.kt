package com.vs.videoscanpdf.ui.pagepicker

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.session.SelectedPage
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
 * UI state for manual page picker screen.
 */
data class ManualPagePickerUiState(
    val isLoading: Boolean = true,
    val videoUri: String? = null,
    val videoDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = Long.MAX_VALUE,
    
    // Playback state
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    
    // Selected pages
    val selectedPages: List<SelectedPage> = emptyList(),
    
    // Step control settings
    val stepSizeMs: Long = 500L, // Default 0.5s
    
    // Capturing state
    val isCapturing: Boolean = false,
    val captureError: String? = null,
    
    // Timeline thumbnails
    val timelineThumbnails: List<Bitmap> = emptyList(),
    
    // Error state
    val error: String? = null
) {
    val selectedCount: Int
        get() = selectedPages.size
    
    val canContinue: Boolean
        get() = selectedPages.isNotEmpty()
    
    val effectiveDurationMs: Long
        get() = if (trimEndMs == Long.MAX_VALUE) videoDurationMs - trimStartMs else trimEndMs - trimStartMs
    
    val currentProgress: Float
        get() = if (effectiveDurationMs > 0) {
            ((currentPositionMs - trimStartMs).toFloat() / effectiveDurationMs).coerceIn(0f, 1f)
        } else 0f
}

/**
 * ViewModel for Manual Page Picker screen.
 * 
 * Handles:
 * - Video playback state
 * - Frame capture at current position
 * - Selected pages management
 * - Step navigation controls
 */
@HiltViewModel
class ManualPagePickerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private var sessionId: String? = null
    private var mediaRetriever: MediaMetadataRetriever? = null
    
    private val _uiState = MutableStateFlow(ManualPagePickerUiState())
    val uiState: StateFlow<ManualPagePickerUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.MANUAL_PICK)
        
        viewModelScope.launch {
            try {
                val session = sessionManager.currentSession ?: return@launch
                val videoUri = session.sourceVideoUri ?: return@launch
                val trimStart = session.trimRange.startMs
                val trimEnd = if (session.trimRange.endMs == Long.MAX_VALUE) {
                    session.videoDurationMs
                } else {
                    session.trimRange.endMs
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videoUri = videoUri,
                    videoDurationMs = session.videoDurationMs,
                    trimStartMs = trimStart,
                    trimEndMs = trimEnd,
                    currentPositionMs = trimStart,
                    selectedPages = session.selectedPages
                )
                
                // Initialize media retriever for frame capture
                initializeMediaRetriever(videoUri)
                
                // Generate timeline thumbnails
                generateTimelineThumbnails(videoUri, trimStart, trimEnd)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load video: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun initializeMediaRetriever(videoUri: String) {
        withContext(Dispatchers.IO) {
            try {
                mediaRetriever = MediaMetadataRetriever().apply {
                    setDataSource(context, Uri.parse(videoUri))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Cannot open video: ${e.message}"
                )
            }
        }
    }
    
    private fun generateTimelineThumbnails(videoUri: String, startMs: Long, endMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, Uri.parse(videoUri))
                
                val thumbnails = mutableListOf<Bitmap>()
                val count = 10 // Number of thumbnails in timeline
                val duration = endMs - startMs
                val interval = duration / count
                
                for (i in 0 until count) {
                    val timeMs = startMs + (i * interval)
                    val timeUs = timeMs * 1000
                    val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    frame?.let {
                        thumbnails.add(Bitmap.createScaledBitmap(it, 80, 60, true))
                    }
                }
                
                retriever.release()
                
                _uiState.value = _uiState.value.copy(timelineThumbnails = thumbnails)
            } catch (e: Exception) {
                // Timeline thumbnails are optional, don't fail
            }
        }
    }
    
    /**
     * Update current playback position.
     */
    fun setCurrentPosition(positionMs: Long) {
        val state = _uiState.value
        val clampedPosition = positionMs.coerceIn(state.trimStartMs, state.trimEndMs)
        _uiState.value = state.copy(currentPositionMs = clampedPosition)
    }
    
    /**
     * Update playing state.
     */
    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }
    
    /**
     * Add page at current frame position.
     */
    fun addPageAtCurrentPosition() {
        val state = _uiState.value
        if (state.isCapturing) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCapturing = true, captureError = null)
            
            try {
                val thumbnail = withContext(Dispatchers.IO) {
                    captureFrameAt(state.currentPositionMs)
                }
                
                if (thumbnail != null) {
                    val page = SelectedPage(
                        id = UUID.randomUUID().toString(),
                        timeMs = state.currentPositionMs,
                        thumbnail = thumbnail
                    )
                    
                    sessionManager.addSelectedPage(page)
                    
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        selectedPages = _uiState.value.selectedPages + page
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        captureError = "Couldn't capture this frame. Try pausing and retry."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    captureError = "Couldn't capture this frame. Try pausing and retry."
                )
            }
        }
    }
    
    private fun captureFrameAt(timeMs: Long): Bitmap? {
        return try {
            val retriever = mediaRetriever ?: return null
            val timeUs = timeMs * 1000
            val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            frame?.let {
                // Create both thumbnail and keep original size reference
                Bitmap.createScaledBitmap(it, 180, 240, true)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Remove a selected page.
     */
    fun removePage(pageId: String) {
        sessionManager.removeSelectedPage(pageId)
        _uiState.value = _uiState.value.copy(
            selectedPages = _uiState.value.selectedPages.filter { it.id != pageId }
        )
    }
    
    /**
     * Step forward by step size.
     */
    fun stepForward() {
        val state = _uiState.value
        val newPosition = (state.currentPositionMs + state.stepSizeMs).coerceAtMost(state.trimEndMs)
        _uiState.value = state.copy(currentPositionMs = newPosition)
    }
    
    /**
     * Step backward by step size.
     */
    fun stepBackward() {
        val state = _uiState.value
        val newPosition = (state.currentPositionMs - state.stepSizeMs).coerceAtLeast(state.trimStartMs)
        _uiState.value = state.copy(currentPositionMs = newPosition)
    }
    
    /**
     * Set step size (0.5s or 1s).
     */
    fun setStepSize(stepMs: Long) {
        _uiState.value = _uiState.value.copy(stepSizeMs = stepMs)
    }
    
    /**
     * Seek to specific progress (0-1).
     */
    fun seekToProgress(progress: Float) {
        val state = _uiState.value
        val duration = state.trimEndMs - state.trimStartMs
        val newPosition = state.trimStartMs + (progress * duration).toLong()
        _uiState.value = state.copy(currentPositionMs = newPosition.coerceIn(state.trimStartMs, state.trimEndMs))
    }
    
    /**
     * Dismiss capture error.
     */
    fun dismissCaptureError() {
        _uiState.value = _uiState.value.copy(captureError = null)
    }
    
    /**
     * Confirm selection and prepare for next step.
     */
    fun confirmSelection() {
        sessionManager.setPageOrder(_uiState.value.selectedPages.map { it.id })
    }
    
    override fun onCleared() {
        super.onCleared()
        mediaRetriever?.release()
    }
}

