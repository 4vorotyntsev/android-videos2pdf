package com.vs.videoscanpdf.ui.picker

import android.graphics.Bitmap

/**
 * UI state for the Frame Picker screen.
 */
data class FramePickerUiState(
    val videoPath: String? = null,
    val videoDurationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedPages: List<SelectedPage> = emptyList(),
    val thumbnails: List<Thumbnail> = emptyList(),
    val currentFrame: Bitmap? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Represents a selected page from the video.
 */
data class SelectedPage(
    val id: String,
    val timeMs: Long,
    val thumbnail: Bitmap? = null
)

/**
 * Thumbnail for the timeline.
 */
data class Thumbnail(
    val timeMs: Long,
    val bitmap: Bitmap
)
