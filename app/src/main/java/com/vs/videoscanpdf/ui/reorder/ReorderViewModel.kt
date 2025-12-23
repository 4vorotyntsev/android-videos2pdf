package com.vs.videoscanpdf.ui.reorder

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import com.vs.videoscanpdf.data.repository.SettingsRepository
import com.vs.videoscanpdf.data.repository.SettingsRepository.OutputProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReorderUiState(
    val pages: List<ReorderPage> = emptyList(),
    val filename: String = "scan",
    val outputProfile: OutputProfile = OutputProfile.BALANCED,
    val pageSize: PageSize = PageSize.AUTO,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ReorderViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReorderUiState())
    val uiState: StateFlow<ReorderUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    
    fun initialize(projectId: String) {
        currentProjectId = projectId
        loadSettings()
        loadPages()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val profile = settingsRepository.getOutputProfile().first()
            val filename = settingsRepository.getPdfFileName().first()
            
            _uiState.update { state ->
                state.copy(
                    outputProfile = profile,
                    filename = generateFilename(filename)
                )
            }
        }
    }
    
    private fun generateFilename(baseName: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "${baseName}_${dateFormat.format(Date())}"
    }
    
    private fun loadPages() {
        viewModelScope.launch {
            try {
                projectRepository.getPagesByProject(currentProjectId).collect { pages ->
                    val pagesWithBitmap = pages.map { page ->
                        val bitmap = withContext(Dispatchers.IO) {
                            try {
                                BitmapFactory.decodeFile(page.imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        ReorderPage(entity = page, bitmap = bitmap)
                    }
                    
                    _uiState.update { state ->
                        state.copy(
                            pages = pagesWithBitmap,
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
    
    fun setFilename(name: String) {
        _uiState.update { state ->
            state.copy(filename = name)
        }
    }
    
    fun setOutputProfile(profile: OutputProfile) {
        _uiState.update { state ->
            state.copy(outputProfile = profile)
        }
    }
    
    fun setPageSize(size: PageSize) {
        _uiState.update { state ->
            state.copy(pageSize = size)
        }
    }
    
    fun moveItem(from: Int, to: Int) {
        val currentList = _uiState.value.pages.toMutableList()
        val item = currentList.removeAt(from)
        currentList.add(to, item)
        
        _uiState.update { state ->
            state.copy(pages = currentList)
        }
    }
    
    fun saveOrder() {
        viewModelScope.launch {
            val pages = _uiState.value.pages.mapIndexed { index, page ->
                page.entity.copy(index = index)
            }
            projectRepository.reorderPages(pages)
        }
    }
}

