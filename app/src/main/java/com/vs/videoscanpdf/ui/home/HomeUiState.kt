package com.vs.videoscanpdf.ui.home

import com.vs.videoscanpdf.data.entities.ExportEntity

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val recentExports: List<ExportEntity> = emptyList(),
    val error: String? = null,
    
    // Permission states
    val hasCameraPermission: Boolean = false,
    val hasMediaPermission: Boolean = false,
    val showCameraPermissionSheet: Boolean = false,
    val showMediaPermissionSheet: Boolean = false
)
