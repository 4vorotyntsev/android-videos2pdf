package com.vs.videoscanpdf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadProjects()
    }
    
    private fun loadProjects() {
        viewModelScope.launch {
            try {
                projectRepository.getAllProjects().collect { projects ->
                    val projectsWithCounts = projects.map { project ->
                        ProjectWithPageCount(
                            project = project,
                            pageCount = projectRepository.getPageCount(project.id)
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        projects = projectsWithCounts,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Creates a new project for recording.
     * Returns the project ID.
     */
    suspend fun createNewProject(): String {
        val title = generateProjectTitle()
        val project = projectRepository.createProject(title)
        return project.id
    }
    
    /**
     * Creates a new project for importing a video.
     * Returns the project ID.
     */
    suspend fun createImportProject(): String {
        val title = generateProjectTitle()
        val project = projectRepository.createProject(title)
        return project.id
    }
    
    /**
     * Deletes a project.
     */
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }
    
    private fun generateProjectTitle(): String {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return "Scan ${dateFormat.format(Date())}"
    }
}
