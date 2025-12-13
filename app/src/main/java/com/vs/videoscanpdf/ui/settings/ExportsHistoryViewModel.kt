package com.vs.videoscanpdf.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing export history.
 */
@HiltViewModel
class ExportsHistoryViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _exports = MutableStateFlow<List<ExportEntity>>(emptyList())
    val exports: StateFlow<List<ExportEntity>> = _exports.asStateFlow()
    
    init {
        loadExports()
    }
    
    private fun loadExports() {
        viewModelScope.launch {
            projectRepository.getAllExports().collect { exportList ->
                _exports.value = exportList
            }
        }
    }
    
    fun deleteExport(export: ExportEntity) {
        viewModelScope.launch {
            projectRepository.deleteExport(export.id, export.pdfPath)
        }
    }
}
