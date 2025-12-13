package com.vs.videoscanpdf.ui.editor

import android.content.Context
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the Page Editor screen.
 */
@HiltViewModel
class PageEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PageEditorUiState())
    val uiState: StateFlow<PageEditorUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    
    fun initialize(projectId: String) {
        currentProjectId = projectId
        loadPages()
    }
    
    private fun loadPages() {
        viewModelScope.launch {
            try {
                projectRepository.getPagesByProject(currentProjectId).collect { pages ->
                    val pagesWithBitmaps = pages.map { page ->
                        PageWithBitmap(
                            entity = page,
                            bitmap = loadBitmap(page.imagePath)
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        pages = pagesWithBitmaps,
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
    
    private suspend fun loadBitmap(path: String) = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            null
        }
    }
    
    fun deletePage(pageId: String) {
        viewModelScope.launch {
            projectRepository.deletePage(pageId, currentProjectId)
        }
    }
    
    fun rotatePage(pageId: String) {
        viewModelScope.launch {
            val page = _uiState.value.pages.find { it.entity.id == pageId } ?: return@launch
            val newRotation = (page.entity.rotation + 90) % 360
            projectRepository.rotatePage(pageId, newRotation)
        }
    }
    
    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val currentPages = _uiState.value.pages.toMutableList()
        val item = currentPages.removeAt(fromIndex)
        currentPages.add(toIndex, item)
        
        // Update UI immediately
        _uiState.value = _uiState.value.copy(pages = currentPages)
        
        // Persist to database
        viewModelScope.launch {
            val reorderedEntities = currentPages.mapIndexed { index, page ->
                page.entity.copy(index = index)
            }
            projectRepository.reorderPages(reorderedEntities)
        }
    }
    
    fun canExport(): Boolean = _uiState.value.pages.isNotEmpty()
}
