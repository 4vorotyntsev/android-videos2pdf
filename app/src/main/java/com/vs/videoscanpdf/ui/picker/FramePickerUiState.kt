package com.vs.videoscanpdf.ui.picker

import android.graphics.Bitmap

/**
 * UI state for the Frame Picker screen (Auto Page Moments).
 */
data class FramePickerUiState(
    val videoPath: String? = null,
    val videoDurationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val selectedPages: List<SelectedPage> = emptyList(),
    val detectedPages: List<DetectedPage> = emptyList(),
    val thumbnails: List<Thumbnail> = emptyList(),
    val currentFrame: Bitmap? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAdvancedSettings: Boolean = false,
    val extractionDensity: Float = 0.5f, // 0 = fewer pages, 1 = more pages
    val isAutoDetecting: Boolean = false,
    val autoDetectionProgress: Float = 0f
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
 * Represents an auto-detected page moment.
 */
data class DetectedPage(
    val timeMs: Long,
    val thumbnail: Bitmap? = null,
    val isSelected: Boolean = true,
    val quality: PageQuality = PageQuality.GOOD
)

/**
 * Quality assessment for detected pages.
 */
enum class PageQuality {
    GOOD,
    BLUR,
    GLARE
}

/**
 * Thumbnail for the timeline.
 */
data class Thumbnail(
    val timeMs: Long,
    val bitmap: Bitmap
)
