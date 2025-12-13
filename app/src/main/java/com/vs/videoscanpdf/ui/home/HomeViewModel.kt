package com.vs.videoscanpdf.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        loadExports()
    }
    
    private fun loadExports() {
        viewModelScope.launch {
            try {
                projectRepository.getAllExports().collect { exports ->
                    _uiState.value = _uiState.value.copy(
                        exports = exports,
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
    
    private fun generateProjectTitle(): String {
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        return "Scan ${dateFormat.format(Date())}"
    }
}

