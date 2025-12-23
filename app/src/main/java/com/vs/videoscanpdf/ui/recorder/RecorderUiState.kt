package com.vs.videoscanpdf.ui.recorder

import java.io.File

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
    val recordedVideoPath: String? = null
)

/**
 * Events from recording.
 */
sealed class RecordingEvent {
    data class Started(val outputFile: File) : RecordingEvent()
    data class Progress(val durationMs: Long) : RecordingEvent()
    data class Completed(val outputFile: File, val durationMs: Long) : RecordingEvent()
    data class Error(val message: String) : RecordingEvent()
}
