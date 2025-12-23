package com.vs.videoscanpdf.ui.recorder

/**
 * UI state for the Recorder screen.
 */
data class RecorderUiState(
    val isRecording: Boolean = false,
    val recordingDurationMs: Long = 0L,
    val isTorchEnabled: Boolean = false,
    val zoomLevel: Float = 0f,
    val showGuidance: Boolean = true,
    val error: String? = null,
    val recordingComplete: Boolean = false,
    val showReviewButtons: Boolean = false,
    val recordedVideoPath: String? = null,
    
    // Stability detection
    val stabilityState: StabilityState = StabilityState.STABLE,
    
    // Storage warning
    val hasLowStorage: Boolean = false
)
