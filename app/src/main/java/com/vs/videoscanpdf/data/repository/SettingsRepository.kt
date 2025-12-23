package com.vs.videoscanpdf.data.repository

import android.content.Context
import android.os.Environment
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Repository for managing app settings using DataStore.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object Keys {
        val PDF_SAVE_PATH = stringPreferencesKey("pdf_save_path")
        val DELETE_VIDEO_AFTER_EXPORT = stringPreferencesKey("delete_video_after_export")
        val USE_MAX_QUALITY = stringPreferencesKey("use_max_quality")
        val USE_GRAYSCALE = stringPreferencesKey("use_grayscale")
        val PDF_FILE_NAME = stringPreferencesKey("pdf_file_name")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val OUTPUT_PROFILE = stringPreferencesKey("output_profile")
        val KEEP_INTERMEDIATE_IMAGES = booleanPreferencesKey("keep_intermediate_images")
    }
    
    /**
     * Output quality profiles for PDF export.
     */
    enum class OutputProfile(val displayName: String, val quality: Int) {
        EMAIL_FRIENDLY("Email-friendly", 60),
        BALANCED("Balanced", 80),
        PRINT_QUALITY("Print quality", 95)
    }
    
    /**
     * Gets the default PDF save path (Documents/VideoScanPDF folder).
     */
    fun getDefaultPdfSavePath(): String {
        // Use app-specific external documents directory which doesn't require permissions
        // and is accessible to the user via USB/File Managers
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
            ?: File(context.filesDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    
    /**
     * Gets the default PDF file name.
     */
    fun getDefaultPdfFileName(): String = "scan"
    
    /**
     * Gets the current PDF save path as a Flow.
     * Returns the default path if not set.
     */
    fun getPdfSavePath(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.PDF_SAVE_PATH] ?: getDefaultPdfSavePath()
        }
    }
    
    /**
     * Gets the current PDF save path synchronously (for ViewModel initialization).
     */
    suspend fun getPdfSavePathSync(): String {
        var result = getDefaultPdfSavePath()
        context.dataStore.data.collect { preferences ->
            result = preferences[Keys.PDF_SAVE_PATH] ?: getDefaultPdfSavePath()
            return@collect
        }
        return result
    }
    
    /**
     * Sets the PDF save path.
     */
    suspend fun setPdfSavePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PDF_SAVE_PATH] = path
        }
    }
    
    /**
     * Gets whether to delete video after export.
     */
    fun getDeleteVideoAfterExport(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.DELETE_VIDEO_AFTER_EXPORT]?.toBoolean() ?: false
        }
    }
    
    /**
     * Sets whether to delete video after export.
     */
    suspend fun setDeleteVideoAfterExport(delete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DELETE_VIDEO_AFTER_EXPORT] = delete.toString()
        }
    }
    
    /**
     * Gets whether to use max video quality.
     */
    fun getUseMaxQuality(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.USE_MAX_QUALITY]?.toBoolean() ?: false
        }
    }
    
    /**
     * Sets whether to use max video quality.
     */
    suspend fun setUseMaxQuality(useMax: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_MAX_QUALITY] = useMax.toString()
        }
    }
    
    /**
     * Gets whether to export PDFs in grayscale.
     */
    fun getUseGrayscale(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.USE_GRAYSCALE]?.toBoolean() ?: false
        }
    }
    
    /**
     * Sets whether to export PDFs in grayscale.
     */
    suspend fun setUseGrayscale(grayscale: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.USE_GRAYSCALE] = grayscale.toString()
        }
    }
    
    /**
     * Gets the custom PDF file name.
     */
    fun getPdfFileName(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.PDF_FILE_NAME] ?: getDefaultPdfFileName()
        }
    }
    
    /**
     * Sets the custom PDF file name.
     */
    suspend fun setPdfFileName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PDF_FILE_NAME] = name
        }
    }
    
    /**
     * Generates a unique file path for the PDF, adding a suffix if file exists.
     */
    fun getUniquePdfFilePath(directory: File, baseName: String): File {
        var file = File(directory, "$baseName.pdf")
        var counter = 1
        
        while (file.exists()) {
            file = File(directory, "${baseName}_$counter.pdf")
            counter++
        }
        
        return file
    }
    
    /**
     * Ensures the PDF save directory exists and is writable.
     * Returns the actual path to use (may fall back to internal storage if external is not available).
     */
    fun ensurePdfSaveDirectory(customPath: String? = null): File {
        val path = customPath ?: getDefaultPdfSavePath()
        val dir = File(path)
        
        try {
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    // Try to fall back to default if custom path failed
                    if (path != getDefaultPdfSavePath()) {
                        return ensurePdfSaveDirectory(null)
                    }
                }
            }
            
            if (dir.canWrite()) {
                return dir
            }
        } catch (e: Exception) {
            // Ignore and fall back
        }
        
        // Final fallback to internal storage which is always writable
        val fallback = File(context.filesDir, "exports")
        if (!fallback.exists()) fallback.mkdirs()
        return fallback
    }
    
    /**
     * Gets whether onboarding has been completed.
     */
    fun getOnboardingCompleted(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] ?: false
        }
    }
    
    /**
     * Sets onboarding as completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
    
    /**
     * Gets the current output profile.
     */
    fun getOutputProfile(): Flow<OutputProfile> {
        return context.dataStore.data.map { preferences ->
            val name = preferences[Keys.OUTPUT_PROFILE] ?: OutputProfile.BALANCED.name
            try {
                OutputProfile.valueOf(name)
            } catch (e: IllegalArgumentException) {
                OutputProfile.BALANCED
            }
        }
    }
    
    /**
     * Sets the output profile.
     */
    suspend fun setOutputProfile(profile: OutputProfile) {
        context.dataStore.edit { preferences ->
            preferences[Keys.OUTPUT_PROFILE] = profile.name
        }
    }
    
    /**
     * Gets whether to keep intermediate images.
     */
    fun getKeepIntermediateImages(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[Keys.KEEP_INTERMEDIATE_IMAGES] ?: false
        }
    }
    
    /**
     * Sets whether to keep intermediate images.
     */
    suspend fun setKeepIntermediateImages(keep: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.KEEP_INTERMEDIATE_IMAGES] = keep
        }
    }
}
