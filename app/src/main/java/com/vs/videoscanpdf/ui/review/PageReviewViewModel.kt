package com.vs.videoscanpdf.ui.review

import androidx.lifecycle.ViewModel
import com.vs.videoscanpdf.data.session.DetectedMoment
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI state for page review screen.
 */
data class PageReviewUiState(
    val pages: List<DetectedMoment> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class PageReviewViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PageReviewUiState())
    val uiState: StateFlow<PageReviewUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        sessionManager.setStatus(SessionStatus.PAGE_REVIEW)
        
        val session = sessionManager.currentSession ?: return
        val selectedPages = session.detectedMoments.filter { it.id in session.selectedMomentIds }
        
        // Sort by page order
        val orderedPages = session.pageOrder.mapNotNull { id ->
            selectedPages.find { it.id == id }
        }.ifEmpty { selectedPages }
        
        _uiState.value = PageReviewUiState(pages = orderedPages)
    }
    
    fun deletePage(pageId: String) {
        sessionManager.toggleMomentSelection(pageId)
        _uiState.value = _uiState.value.copy(
            pages = _uiState.value.pages.filter { it.id != pageId }
        )
    }
    
    fun autoFixAll() {
        // In real implementation, apply auto-enhancement to all pages
        // For now, just mark as processed
    }
    
    fun confirmReview() {
        // Update session with final page order
        val pageOrder = _uiState.value.pages.map { it.id }
        sessionManager.setPageOrder(pageOrder)
    }
}
