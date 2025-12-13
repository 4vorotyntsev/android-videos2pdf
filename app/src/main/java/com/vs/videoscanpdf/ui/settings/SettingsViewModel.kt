package com.vs.videoscanpdf.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val pdfSavePath: String = "",
    val pdfFileName: String = "scan",
    val deleteVideoAfterExport: Boolean = false,
    val useMaxQuality: Boolean = false,
    val useGrayscale: Boolean = false,
    val isEditingPath: Boolean = false,
    val isEditingFileName: Boolean = false
)

/**
 * ViewModel for the Settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getPdfSavePath().collect { path ->
                _uiState.value = _uiState.value.copy(pdfSavePath = path)
            }
        }
        viewModelScope.launch {
            settingsRepository.getPdfFileName().collect { name ->
                _uiState.value = _uiState.value.copy(pdfFileName = name)
            }
        }
        viewModelScope.launch {
            settingsRepository.getDeleteVideoAfterExport().collect { delete ->
                _uiState.value = _uiState.value.copy(deleteVideoAfterExport = delete)
            }
        }
        viewModelScope.launch {
            settingsRepository.getUseMaxQuality().collect { useMax ->
                _uiState.value = _uiState.value.copy(useMaxQuality = useMax)
            }
        }
        viewModelScope.launch {
            settingsRepository.getUseGrayscale().collect { grayscale ->
                _uiState.value = _uiState.value.copy(useGrayscale = grayscale)
            }
        }
    }
    
    fun setPdfSavePath(path: String) {
        viewModelScope.launch {
            settingsRepository.setPdfSavePath(path)
            _uiState.value = _uiState.value.copy(pdfSavePath = path, isEditingPath = false)
        }
    }
    
    fun setPdfFileName(name: String) {
        viewModelScope.launch {
            val sanitizedName = name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            settingsRepository.setPdfFileName(sanitizedName)
            _uiState.value = _uiState.value.copy(pdfFileName = sanitizedName, isEditingFileName = false)
        }
    }
    
    fun setDeleteVideoAfterExport(delete: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDeleteVideoAfterExport(delete)
            _uiState.value = _uiState.value.copy(deleteVideoAfterExport = delete)
        }
    }
    
    fun setUseMaxQuality(useMax: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseMaxQuality(useMax)
            _uiState.value = _uiState.value.copy(useMaxQuality = useMax)
        }
    }
    
    fun setUseGrayscale(grayscale: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseGrayscale(grayscale)
            _uiState.value = _uiState.value.copy(useGrayscale = grayscale)
        }
    }
    
    fun startEditingPath() {
        _uiState.value = _uiState.value.copy(isEditingPath = true)
    }
    
    fun cancelEditingPath() {
        _uiState.value = _uiState.value.copy(isEditingPath = false)
    }
    
    fun startEditingFileName() {
        _uiState.value = _uiState.value.copy(isEditingFileName = true)
    }
    
    fun cancelEditingFileName() {
        _uiState.value = _uiState.value.copy(isEditingFileName = false)
    }
    
    fun resetToDefaultPath() {
        viewModelScope.launch {
            val defaultPath = settingsRepository.getDefaultPdfSavePath()
            settingsRepository.setPdfSavePath(defaultPath)
            _uiState.value = _uiState.value.copy(pdfSavePath = defaultPath, isEditingPath = false)
        }
    }
    
    fun resetToDefaultFileName() {
        viewModelScope.launch {
            val defaultName = settingsRepository.getDefaultPdfFileName()
            settingsRepository.setPdfFileName(defaultName)
            _uiState.value = _uiState.value.copy(pdfFileName = defaultName, isEditingFileName = false)
        }
    }
}
