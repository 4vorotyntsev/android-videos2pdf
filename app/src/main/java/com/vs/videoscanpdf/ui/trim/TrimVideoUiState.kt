package com.vs.videoscanpdf.ui.trim

import android.graphics.Bitmap

/**
 * UI state for trim video screen.
 */
data class TrimVideoUiState(
    val videoUri: String? = null,
    val videoDurationMs: Long = 0L,
    
    // Trim range
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    
    // Playback state
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    
    // Timeline thumbnails
    val thumbnails: List<Bitmap> = emptyList(),
    val isLoadingThumbnails: Boolean = false,
    
    // Validation
    val isTrimValid: Boolean = true,
    val trimDurationMs: Long = 0L,
    
    // Loading states
    val isLoading: Boolean = true,
    val error: String? = null
) {
    companion object {
        const val MIN_TRIM_DURATION_MS = 2000L // 2 seconds minimum
    }
    
    val effectiveDuration: Long
        get() = trimEndMs - trimStartMs
    
    val startProgress: Float
        get() = if (videoDurationMs > 0) trimStartMs.toFloat() / videoDurationMs else 0f
    
    val endProgress: Float
        get() = if (videoDurationMs > 0) trimEndMs.toFloat() / videoDurationMs else 1f
    
    val currentProgress: Float
        get() = if (videoDurationMs > 0) currentPositionMs.toFloat() / videoDurationMs else 0f
}

