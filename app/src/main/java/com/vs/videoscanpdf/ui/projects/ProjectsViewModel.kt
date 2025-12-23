package com.vs.videoscanpdf.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.ProjectEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()
    
    init {
        loadProjects()
    }
    
    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _uiState.update { state ->
                    state.copy(
                        projects = projects.sortedByDescending { it.updatedAt },
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            projectRepository.deleteProject(project.id)
        }
    }
    
    fun renameProject(project: ProjectEntity, newName: String) {
        viewModelScope.launch {
            projectRepository.updateProject(
                project.copy(
                    title = newName,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

