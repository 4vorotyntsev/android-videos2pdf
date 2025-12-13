package com.vs.videoscanpdf.ui.home

import com.vs.videoscanpdf.data.entities.ExportEntity

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val exports: List<ExportEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
