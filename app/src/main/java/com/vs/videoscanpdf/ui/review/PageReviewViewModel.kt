package com.vs.videoscanpdf.ui.review

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PageReviewUiState(
    val pages: List<PageWithPreview> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PageReviewViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PageReviewUiState())
    val uiState: StateFlow<PageReviewUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    
    fun initialize(projectId: String) {
        currentProjectId = projectId
        loadPages()
    }
    
    private fun loadPages() {
        viewModelScope.launch {
            try {
                projectRepository.getPagesByProject(currentProjectId).collect { pages ->
                    val pagesWithPreview = pages.map { page ->
                        val bitmap = withContext(Dispatchers.IO) {
                            try {
                                BitmapFactory.decodeFile(page.imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        // Assign random quality tag for demo (in real app, would analyze image)
                        val quality = when {
                            page.rotation != 0 -> PageQualityTag.TILTED
                            else -> PageQualityTag.GOOD
                        }
                        
                        PageWithPreview(
                            entity = page,
                            bitmap = bitmap,
                            quality = quality
                        )
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            pages = pagesWithPreview,
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
    
    fun deletePage(pageId: String) {
        viewModelScope.launch {
            projectRepository.deletePage(pageId, currentProjectId)
        }
    }
}

