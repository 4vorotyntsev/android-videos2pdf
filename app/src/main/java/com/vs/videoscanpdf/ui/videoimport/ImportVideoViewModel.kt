package com.vs.videoscanpdf.ui.videoimport

import android.content.Context
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

/**
 * Import error types.
 */
enum class ImportError {
    TOO_SHORT,
    UNSUPPORTED_FORMAT,
    CANNOT_READ
}

/**
 * UI state for import video screen.
 */
data class ImportVideoUiState(
    val videoUri: Uri? = null,
    val durationMs: Long = 0L,
    val isValidating: Boolean = false,
    val isConfirmed: Boolean = false,
    val error: ImportError? = null
)

@HiltViewModel
class ImportVideoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    companion object {
        private const val MIN_DURATION_MS = 2000L // 2 seconds minimum
    }
    
    private var sessionId: String? = null
    
    private val _uiState = MutableStateFlow(ImportVideoUiState())
    val uiState: StateFlow<ImportVideoUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.IMPORTING)
    }
    
    /**
     * Validate the selected video.
     */
    fun validateVideo(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isValidating = true,
                error = null
            )
            
            try {
                val result = withContext(Dispatchers.IO) {
                    validateVideoInternal(uri)
                }
                
                result.fold(
                    onSuccess = { duration ->
                        _uiState.value = _uiState.value.copy(
                            videoUri = uri,
                            durationMs = duration,
                            isValidating = false
                        )
                        
                        // Update session with video info
                        sessionManager.setSourceVideo(
                            uri = uri.toString(),
                            durationMs = duration,
                            isImported = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isValidating = false,
                            error = when (error) {
                                is TooShortException -> ImportError.TOO_SHORT
                                is UnsupportedFormatException -> ImportError.UNSUPPORTED_FORMAT
                                else -> ImportError.CANNOT_READ
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isValidating = false,
                    error = ImportError.CANNOT_READ
                )
            }
        }
    }
    
    /**
     * Confirm the video and proceed.
     */
    fun confirmVideo() {
        _uiState.value = _uiState.value.copy(isConfirmed = true)
    }
    
    private fun validateVideoInternal(uri: Uri): Result<Long> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            
            // Get duration
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            
            // Check if we can actually read frames
            val frame = retriever.getFrameAtTime(0)
            
            retriever.release()
            
            if (frame == null) {
                Result.failure(UnsupportedFormatException())
            } else if (duration < MIN_DURATION_MS) {
                Result.failure(TooShortException())
            } else {
                Result.success(duration)
            }
        } catch (e: Exception) {
            Result.failure(CannotReadException())
        }
    }
    
    private class TooShortException : Exception()
    private class UnsupportedFormatException : Exception()
    private class CannotReadException : Exception()
}

