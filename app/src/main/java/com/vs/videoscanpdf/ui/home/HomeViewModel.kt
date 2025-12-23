package com.vs.videoscanpdf.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import com.vs.videoscanpdf.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Home screen.
 * 
 * Manages:
 * - Recent exports display (limited to 3)
 * - Session creation for new scans
 * - Permission state checking
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadRecentExports()
        checkPermissions()
    }
    
    private fun loadRecentExports() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                projectRepository.getAllExports().collect { exports ->
                    _uiState.value = _uiState.value.copy(
                        recentExports = exports.take(3), // Limit to 3 as per spec
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
    
    private fun checkPermissions() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasMediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        _uiState.value = _uiState.value.copy(
            hasCameraPermission = hasCameraPermission,
            hasMediaPermission = hasMediaPermission
        )
    }
    
    /**
     * Refresh permission state after permission request.
     */
    fun refreshPermissions() {
        checkPermissions()
    }
    
    /**
     * Start a new scanning session.
     * Returns the session ID.
     */
    fun startNewSession(): String {
        val session = sessionManager.startSession()
        return session.id
    }
    
    /**
     * Check if camera permission is granted.
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if media permission is granted.
     */
    fun hasMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Show permission sheet for camera.
     */
    fun showCameraPermissionSheet() {
        _uiState.value = _uiState.value.copy(showCameraPermissionSheet = true)
    }
    
    /**
     * Show permission sheet for media.
     */
    fun showMediaPermissionSheet() {
        _uiState.value = _uiState.value.copy(showMediaPermissionSheet = true)
    }
    
    /**
     * Dismiss permission sheets.
     */
    fun dismissPermissionSheet() {
        _uiState.value = _uiState.value.copy(
            showCameraPermissionSheet = false,
            showMediaPermissionSheet = false
        )
    }
    
    /**
     * Delete an export.
     */
    fun deleteExport(export: ExportEntity) {
        viewModelScope.launch {
            projectRepository.deleteExport(export.id, export.pdfPath)
        }
    }
}
