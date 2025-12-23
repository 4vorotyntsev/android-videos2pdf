package com.vs.videoscanpdf.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.vs.videoscanpdf.data.session.PageEdit
import com.vs.videoscanpdf.data.session.PageFilter
import com.vs.videoscanpdf.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI state for single page editor.
 */
data class SinglePageEditorUiState(
    val pageId: String = "",
    val previewBitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val selectedTool: EditorTool? = null,
    val selectedFilter: PageFilter = PageFilter.DOCUMENT,
    val rotation: Int = 0,
    val hasChanges: Boolean = false
)

@HiltViewModel
class SinglePageEditorViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private var sessionId: String? = null
    
    private val _uiState = MutableStateFlow(SinglePageEditorUiState())
    val uiState: StateFlow<SinglePageEditorUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String, pageId: String) {
        this.sessionId = sessionId
        
        val session = sessionManager.currentSession ?: return
        val moment = session.detectedMoments.find { it.id == pageId }
        val existingEdit = session.pageEdits[pageId]
        
        _uiState.value = SinglePageEditorUiState(
            pageId = pageId,
            previewBitmap = moment?.thumbnail,
            originalBitmap = moment?.thumbnail,
            selectedFilter = existingEdit?.filter ?: PageFilter.DOCUMENT,
            rotation = existingEdit?.rotation ?: 0
        )
    }
    
    fun selectTool(tool: EditorTool) {
        when (tool) {
            EditorTool.AUTO -> applyAutoFix()
            EditorTool.RESET -> resetChanges()
            else -> {
                _uiState.value = _uiState.value.copy(selectedTool = tool)
            }
        }
    }
    
    fun applyFilter(filter: PageFilter) {
        _uiState.value = _uiState.value.copy(
            selectedFilter = filter,
            hasChanges = true
        )
        // In real implementation, apply filter to bitmap
    }
    
    fun rotate() {
        val newRotation = (_uiState.value.rotation + 90) % 360
        _uiState.value = _uiState.value.copy(
            rotation = newRotation,
            hasChanges = true
        )
        // In real implementation, rotate bitmap
    }
    
    private fun applyAutoFix() {
        _uiState.value = _uiState.value.copy(
            selectedFilter = PageFilter.DOCUMENT,
            hasChanges = true
        )
        // In real implementation, apply auto-enhancement
    }
    
    private fun resetChanges() {
        _uiState.value = _uiState.value.copy(
            previewBitmap = _uiState.value.originalBitmap,
            selectedFilter = PageFilter.DOCUMENT,
            rotation = 0,
            hasChanges = false,
            selectedTool = null
        )
    }
    
    fun saveChanges() {
        val state = _uiState.value
        if (state.hasChanges) {
            sessionManager.applyPageEdit(
                momentId = state.pageId,
                edit = PageEdit(
                    momentId = state.pageId,
                    rotation = state.rotation,
                    filter = state.selectedFilter,
                    hasAutoFix = true
                )
            )
        }
    }
}
