package com.vs.videoscanpdf.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.SettingsRepository
import com.vs.videoscanpdf.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Splash screen state.
 */
data class SplashUiState(
    val isInitialized: Boolean = false,
    val hadLeftoverSession: Boolean = false,
    val showCleanupNotice: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    val onboardingCompleted: Flow<Boolean> = settingsRepository.getOnboardingCompleted()
    
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()
    
    init {
        initialize()
    }
    
    /**
     * Initialize the app - cleanup leftover sessions, verify temp dir, etc.
     */
    private fun initialize() {
        viewModelScope.launch {
            try {
                // Cleanup any leftover temp files from previous sessions
                val hadLeftovers = sessionManager.initializeAndCleanup()
                
                _uiState.value = SplashUiState(
                    isInitialized = true,
                    hadLeftoverSession = hadLeftovers,
                    showCleanupNotice = hadLeftovers
                )
            } catch (e: Exception) {
                _uiState.value = SplashUiState(
                    isInitialized = true,
                    error = "Failed to initialize: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Dismiss the cleanup notice.
     */
    fun dismissCleanupNotice() {
        _uiState.value = _uiState.value.copy(showCleanupNotice = false)
    }
}
