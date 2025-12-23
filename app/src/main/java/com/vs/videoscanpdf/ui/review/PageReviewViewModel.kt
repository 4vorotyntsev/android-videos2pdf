package com.vs.videoscanpdf.ui.review

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.vs.videoscanpdf.data.session.DetectedMoment
import com.vs.videoscanpdf.data.session.SelectedPage
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Unified page item for review screen.
 * Works with both DetectedMoment (legacy) and SelectedPage (manual selection).
 */
data class ReviewPageItem(
    val id: String,
    val timeMs: Long,
    val thumbnail: Bitmap?,
    val qualityScore: Float = 1f
)

/**
 * UI state for page review screen.
 */
data class PageReviewUiState(
    val pages: List<ReviewPageItem> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel for Page Review screen.
 * 
 * Manual Selection Edition:
 * - Works with manually selected pages (SelectedPage)
 * - Supports drag-to-reorder
 * - Supports delete
 * - Falls back to DetectedMoment for compatibility
 */
@HiltViewModel
class PageReviewViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PageReviewUiState())
    val uiState: StateFlow<PageReviewUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        sessionManager.setStatus(SessionStatus.PAGE_REVIEW)
        
        val session = sessionManager.currentSession ?: return
        
        // Manual Selection Edition: Use selectedPages if available
        val reviewPages: List<ReviewPageItem> = if (session.selectedPages.isNotEmpty()) {
            session.selectedPages.map { page ->
                ReviewPageItem(
                    id = page.id,
                    timeMs = page.timeMs,
                    thumbnail = page.thumbnail
                )
            }
        } else {
            // Legacy fallback: Use detected moments
            val selectedPages = session.detectedMoments.filter { it.id in session.selectedMomentIds }
            val orderedPages = session.pageOrder.mapNotNull { id ->
                selectedPages.find { it.id == id }
            }.ifEmpty { selectedPages }
            
            orderedPages.map { moment ->
                ReviewPageItem(
                    id = moment.id,
                    timeMs = moment.timeMs,
                    thumbnail = moment.thumbnail,
                    qualityScore = moment.qualityScore
                )
            }
        }
        
        _uiState.value = PageReviewUiState(pages = reviewPages)
    }
    
    /**
     * Delete a page from the session.
     */
    fun deletePage(pageId: String) {
        val session = sessionManager.currentSession ?: return
        
        if (session.selectedPages.isNotEmpty()) {
            // Manual selection: Remove from selectedPages
            sessionManager.removeSelectedPage(pageId)
        } else {
            // Legacy: Toggle moment selection
            @Suppress("DEPRECATION")
            sessionManager.toggleMomentSelection(pageId)
        }
        
        _uiState.value = _uiState.value.copy(
            pages = _uiState.value.pages.filter { it.id != pageId }
        )
    }
    
    /**
     * Move a page from one position to another (drag reorder).
     */
    fun movePage(fromIndex: Int, toIndex: Int) {
        val pages = _uiState.value.pages.toMutableList()
        if (fromIndex !in pages.indices || toIndex !in pages.indices) return
        
        val page = pages.removeAt(fromIndex)
        pages.add(toIndex, page)
        
        _uiState.value = _uiState.value.copy(pages = pages)
        
        // Update session
        val session = sessionManager.currentSession ?: return
        if (session.selectedPages.isNotEmpty()) {
            sessionManager.reorderSelectedPages(fromIndex, toIndex)
        } else {
            sessionManager.setPageOrder(pages.map { it.id })
        }
    }
    
    /**
     * Apply auto-fix to all pages.
     */
    fun autoFixAll() {
        // In real implementation, apply auto-enhancement to all pages
        // For now, just mark as processed
    }
    
    /**
     * Confirm review and update session with final page order.
     */
    fun confirmReview() {
        val pageOrder = _uiState.value.pages.map { it.id }
        sessionManager.setPageOrder(pageOrder)
    }
}
