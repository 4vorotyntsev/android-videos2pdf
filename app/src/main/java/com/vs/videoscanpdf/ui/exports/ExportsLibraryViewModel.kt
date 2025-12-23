package com.vs.videoscanpdf.ui.exports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UI state for exports library screen.
 */
data class ExportsLibraryUiState(
    val exports: List<ExportEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val filteredExports: List<ExportEntity>
        get() = if (searchQuery.isBlank()) {
            exports
        } else {
            exports.filter { export ->
                export.pdfPath.substringAfterLast("/")
                    .contains(searchQuery, ignoreCase = true)
            }
        }
}

@HiltViewModel
class ExportsLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportsLibraryUiState())
    val uiState: StateFlow<ExportsLibraryUiState> = _uiState.asStateFlow()
    
    init {
        loadExports()
    }
    
    private fun loadExports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                projectRepository.getAllExports().collect { exports ->
                    _uiState.value = _uiState.value.copy(
                        exports = exports.sortedByDescending { it.createdAt },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun shareExport(export: ExportEntity) {
        try {
            val file = File(export.pdfPath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to share: ${e.message}")
        }
    }
    
    fun renameExport(export: ExportEntity, newName: String) {
        viewModelScope.launch {
            try {
                val oldFile = File(export.pdfPath)
                val newFile = File(oldFile.parentFile, "$newName.pdf")
                
                if (oldFile.renameTo(newFile)) {
                    projectRepository.updateExportPath(export.id, newFile.absolutePath)
                } else {
                    _uiState.value = _uiState.value.copy(error = "Failed to rename file")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to rename: ${e.message}")
            }
        }
    }
    
    fun deleteExport(export: ExportEntity) {
        viewModelScope.launch {
            projectRepository.deleteExport(export.id, export.pdfPath)
        }
    }
}

