package com.vs.videoscanpdf.data.session

import android.graphics.Bitmap
import java.io.File
import java.util.UUID

/**
 * Session status representing the current state in the scanning flow.
 */
enum class SessionStatus {
    IDLE,
    RECORDING,
    REVIEW_VIDEO,
    IMPORTING,
    TRIMMING,
    MANUAL_PICK,  // Manual page selection (user scrubs and picks pages)
    @Deprecated("Use MANUAL_PICK instead - Manual Selection Edition")
    AUTO_MOMENTS, // Legacy: kept for backward compatibility
    PAGE_REVIEW,
    PROCESSING,
    EXPORT_SETUP,
    EXPORT_RESULT,
    DISCARDED
}

/**
 * Represents an excluded page moment with reason.
 */
data class ExcludedMoment(
    val timeMs: Long,
    val reason: ExclusionReason,
    val thumbnail: Bitmap? = null
)

enum class ExclusionReason {
    BLUR,
    GLARE,
    DUPLICATE,
    TOO_DARK,
    TOO_BRIGHT,
    MOTION_BLUR
}

/**
 * Represents a detected page moment.
 */
data class DetectedMoment(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val thumbnail: Bitmap? = null,
    val isSelected: Boolean = true,
    val qualityScore: Float = 1f
)

/**
 * Represents a manually selected page.
 */
data class SelectedPage(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val thumbnail: Bitmap? = null,
    val capturedAt: Long = System.currentTimeMillis()
)

/**
 * Represents page edits applied by the user.
 */
data class PageEdit(
    val momentId: String,
    val rotation: Int = 0,
    val cropRect: android.graphics.RectF? = null,
    val perspectivePoints: List<android.graphics.PointF>? = null,
    val filter: PageFilter = PageFilter.DOCUMENT,
    val hasAutoFix: Boolean = false
)

enum class PageFilter {
    DOCUMENT,   // Default: contrast normalization, mild sharpen
    ORIGINAL,   // No processing
    BLACK_WHITE // Grayscale with high contrast
}

/**
 * Trim range for video.
 */
data class TrimRange(
    val startMs: Long = 0L,
    val endMs: Long = Long.MAX_VALUE
)

/**
 * Ephemeral session data that exists only in memory during the scanning flow.
 * Any navigation away from the flow discards this session (Mode B - strict no-drafts).
 */
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val status: SessionStatus = SessionStatus.IDLE,
    val createdAt: Long = System.currentTimeMillis(),
    
    // Video source
    val sourceVideoUri: String? = null,
    val videoDurationMs: Long = 0L,
    val isImportedVideo: Boolean = false,
    
    // Trim
    val trimRange: TrimRange = TrimRange(),
    
    // Manual page selection (new)
    val selectedPages: List<SelectedPage> = emptyList(),
    
    // Legacy: Detected moments (kept for compatibility)
    val detectedMoments: List<DetectedMoment> = emptyList(),
    val excludedMoments: List<ExcludedMoment> = emptyList(),
    
    // Selected pages and edits
    val selectedMomentIds: Set<String> = emptySet(),
    val pageEdits: Map<String, PageEdit> = emptyMap(),
    val pageOrder: List<String> = emptyList(), // Ordered page IDs
    
    // Processing
    val processedImages: Map<String, File> = emptyMap(),
    val processingProgress: Float = 0f,
    val currentProcessingStage: ProcessingStage = ProcessingStage.IDLE,
    
    // Temp directory for this session
    val tempDir: File? = null,
    
    // Export result
    val exportedPdfPath: String? = null,
    val exportedFileSize: Long = 0L
) {
    val selectedMoments: List<DetectedMoment>
        get() = detectedMoments.filter { it.id in selectedMomentIds }
    
    val selectedCount: Int
        get() = selectedPages.size.takeIf { it > 0 } ?: selectedMomentIds.size
    
    val excludedCount: Int
        get() = excludedMoments.size
    
    val effectiveDurationMs: Long
        get() = if (trimRange.endMs == Long.MAX_VALUE) {
            videoDurationMs - trimRange.startMs
        } else {
            trimRange.endMs - trimRange.startMs
        }
    
    val isValidForProcessing: Boolean
        get() = (selectedPages.isNotEmpty() || selectedMomentIds.isNotEmpty()) && sourceVideoUri != null
}

enum class ProcessingStage {
    IDLE,
    EXTRACTING_FRAMES,   // Extracting selected frames from video
    ENHANCING_IMAGES,    // Document filter, perspective correction
    GENERATING_PDF,
    COMPLETE
}

