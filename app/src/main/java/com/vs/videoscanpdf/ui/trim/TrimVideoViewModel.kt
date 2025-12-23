package com.vs.videoscanpdf.ui.trim

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import javax.inject.Inject

@HiltViewModel
class TrimVideoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    companion object {
        private const val THUMBNAIL_COUNT = 8
    }
    
    private var sessionId: String? = null
    
    private val _uiState = MutableStateFlow(TrimVideoUiState())
    val uiState: StateFlow<TrimVideoUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.TRIMMING)
        
        // Load video info from session
        sessionManager.currentSession?.let { session ->
            val uri = session.sourceVideoUri
            val duration = session.videoDurationMs
            
            if (uri != null && duration > 0) {
                _uiState.value = _uiState.value.copy(
                    videoUri = uri,
                    videoDurationMs = duration,
                    trimStartMs = session.trimRange.startMs,
                    trimEndMs = if (session.trimRange.endMs == Long.MAX_VALUE) duration else session.trimRange.endMs,
                    trimDurationMs = duration,
                    isLoading = false
                )
                
                loadThumbnails(uri, duration)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Video not found"
                )
            }
        }
    }
    
    private fun loadThumbnails(videoUri: String, durationMs: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingThumbnails = true)
            
            try {
                val thumbnails = withContext(Dispatchers.IO) {
                    extractThumbnails(videoUri, durationMs)
                }
                _uiState.value = _uiState.value.copy(
                    thumbnails = thumbnails,
                    isLoadingThumbnails = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingThumbnails = false)
            }
        }
    }
    
    private fun extractThumbnails(videoUri: String, durationMs: Long): List<Bitmap> {
        val thumbnails = mutableListOf<Bitmap>()
        val retriever = MediaMetadataRetriever()
        
        try {
            retriever.setDataSource(context, Uri.parse(videoUri))
            
            val interval = durationMs / THUMBNAIL_COUNT
            for (i in 0 until THUMBNAIL_COUNT) {
                val timeUs = (i * interval * 1000) // Convert ms to us
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                frame?.let {
                    // Scale down for thumbnails
                    val scaled = Bitmap.createScaledBitmap(it, 120, 160, true)
                    thumbnails.add(scaled)
                }
            }
        } finally {
            retriever.release()
        }
        
        return thumbnails
    }
    
    /**
     * Set trim start position.
     */
    fun setTrimStart(progress: Float) {
        val currentState = _uiState.value
        val newStartMs = (progress * currentState.videoDurationMs).toLong()
        
        // Ensure start is before end and maintains minimum duration
        val maxStart = currentState.trimEndMs - TrimVideoUiState.MIN_TRIM_DURATION_MS
        val clampedStart = newStartMs.coerceIn(0, maxStart.coerceAtLeast(0))
        
        _uiState.value = currentState.copy(
            trimStartMs = clampedStart,
            trimDurationMs = currentState.trimEndMs - clampedStart,
            isTrimValid = (currentState.trimEndMs - clampedStart) >= TrimVideoUiState.MIN_TRIM_DURATION_MS
        )
    }
    
    /**
     * Set trim end position.
     */
    fun setTrimEnd(progress: Float) {
        val currentState = _uiState.value
        val newEndMs = (progress * currentState.videoDurationMs).toLong()
        
        // Ensure end is after start and maintains minimum duration
        val minEnd = currentState.trimStartMs + TrimVideoUiState.MIN_TRIM_DURATION_MS
        val clampedEnd = newEndMs.coerceIn(minEnd, currentState.videoDurationMs)
        
        _uiState.value = currentState.copy(
            trimEndMs = clampedEnd,
            trimDurationMs = clampedEnd - currentState.trimStartMs,
            isTrimValid = (clampedEnd - currentState.trimStartMs) >= TrimVideoUiState.MIN_TRIM_DURATION_MS
        )
    }
    
    /**
     * Update current playback position.
     */
    fun setCurrentPosition(positionMs: Long) {
        _uiState.value = _uiState.value.copy(currentPositionMs = positionMs)
    }
    
    /**
     * Set playing state.
     */
    fun setPlaying(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }
    
    /**
     * Confirm trim and update session.
     */
    fun confirmTrim() {
        val state = _uiState.value
        sessionManager.setTrimRange(state.trimStartMs, state.trimEndMs)
    }
    
    /**
     * Skip trimming (use full video).
     */
    fun skipTrim() {
        val state = _uiState.value
        sessionManager.setTrimRange(0, state.videoDurationMs)
    }
}

