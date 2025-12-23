package com.vs.videoscanpdf

import android.app.Application
import com.vs.videoscanpdf.data.session.SessionCleanupWorker
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for VideoScan PDF.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
class VideoScanPdfApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Schedule cleanup of any leftover session temp files
        SessionCleanupWorker.schedule(this)
    }
}
