package com.vs.videoscanpdf.ui.home

import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.entities.ProjectEntity

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val exports: List<ExportEntity> = emptyList(),
    val recentProjects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
