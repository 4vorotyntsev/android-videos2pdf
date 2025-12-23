package com.vs.videoscanpdf.data.session

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Worker to clean up leftover session temp files.
 * Runs on app startup and periodically to ensure no orphaned files remain.
 */
class SessionCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "SessionCleanupWorker"
        private const val WORK_NAME = "session_cleanup"
        private const val TEMP_DIR_NAME = "session_temp"
        
        /**
         * Schedule cleanup work to run.
         */
        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<SessionCleanupWorker>()
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting session cleanup")
        
        return try {
            val baseDir = File(applicationContext.cacheDir, TEMP_DIR_NAME)
            if (baseDir.exists()) {
                val children = baseDir.listFiles() ?: emptyArray()
                var cleanedCount = 0
                var cleanedSize = 0L
                
                children.forEach { dir ->
                    val size = calculateSize(dir)
                    if (deleteRecursively(dir)) {
                        cleanedCount++
                        cleanedSize += size
                    }
                }
                
                if (cleanedCount > 0) {
                    Log.d(TAG, "Cleaned up $cleanedCount session(s), freed ${formatSize(cleanedSize)}")
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup failed", e)
            Result.failure()
        }
    }
    
    private fun calculateSize(file: File): Long {
        return if (file.isDirectory) {
            file.listFiles()?.sumOf { calculateSize(it) } ?: 0L
        } else {
            file.length()
        }
    }
    
    private fun deleteRecursively(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        return file.delete()
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}

