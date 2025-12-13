package com.vs.videoscanpdf.ui.home

import com.vs.videoscanpdf.data.entities.ProjectEntity

/**
 * UI state for the Home screen.
 */
data class HomeUiState(
    val projects: List<ProjectWithPageCount> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

/**
 * Project with its page count for display.
 */
data class ProjectWithPageCount(
    val project: ProjectEntity,
    val pageCount: Int
)
