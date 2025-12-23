package com.vs.videoscanpdf.ui.splash

import androidx.lifecycle.ViewModel
import com.vs.videoscanpdf.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    
    val onboardingCompleted: Flow<Boolean> = settingsRepository.getOnboardingCompleted()
}

