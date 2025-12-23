package com.vs.videoscanpdf.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.FilterType
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SinglePageEditorUiState(
    val bitmap: Bitmap? = null,
    val rotation: Int = 0,
    val currentFilter: EditorFilter = EditorFilter.ORIGINAL,
    val originalRotation: Int = 0,
    val originalFilter: EditorFilter = EditorFilter.ORIGINAL,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SinglePageEditorViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SinglePageEditorUiState())
    val uiState: StateFlow<SinglePageEditorUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    private lateinit var currentPageId: String
    
    fun initialize(projectId: String, pageId: String) {
        currentProjectId = projectId
        currentPageId = pageId
        loadPage()
    }
    
    private fun loadPage() {
        viewModelScope.launch {
            try {
                val pages = projectRepository.getPagesByProjectSync(currentProjectId)
                val page = pages.find { it.id == currentPageId }
                
                if (page != null) {
                    val bitmap = withContext(Dispatchers.IO) {
                        BitmapFactory.decodeFile(page.imagePath)
                    }
                    
                    val filter = try {
                        EditorFilter.entries.find { it.filterType.name == page.filterType }
                            ?: EditorFilter.ORIGINAL
                    } catch (e: Exception) {
                        EditorFilter.ORIGINAL
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            bitmap = bitmap,
                            rotation = page.rotation,
                            currentFilter = filter,
                            originalRotation = page.rotation,
                            originalFilter = filter,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            error = "Page not found",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    fun rotateRight() {
        _uiState.update { state ->
            state.copy(rotation = (state.rotation + 90) % 360)
        }
    }
    
    fun setFilter(filter: EditorFilter) {
        _uiState.update { state ->
            state.copy(currentFilter = filter)
        }
    }
    
    fun reset() {
        _uiState.update { state ->
            state.copy(
                rotation = state.originalRotation,
                currentFilter = state.originalFilter
            )
        }
    }
    
    fun saveChanges() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Update page entity with new rotation and filter
            val pages = projectRepository.getPagesByProjectSync(currentProjectId)
            val page = pages.find { it.id == currentPageId } ?: return@launch
            
            val updatedPage = page.copy(
                rotation = state.rotation,
                filterType = state.currentFilter.filterType.name
            )
            
            projectRepository.updatePage(updatedPage)
        }
    }
}

