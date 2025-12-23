package com.vs.videoscanpdf.data.session

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Events emitted by SessionManager for UI to observe.
 */
sealed class SessionEvent {
    data object SessionDiscarded : SessionEvent()
    data class PreviousScanCleared(val hadLeftovers: Boolean) : SessionEvent()
    data class SessionError(val message: String) : SessionEvent()
}

/**
 * Singleton managing ephemeral session state for Mode B (strict no-drafts).
 * 
 * Key behaviors:
 * - Session lives only in memory, no persistence for in-progress work
 * - Any navigation away from flow discards session immediately
 * - ProcessLifecycleOwner observer detects backgrounding
 * - Cleanup of leftover temp files on app start
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val TEMP_DIR_NAME = "session_temp"
        private const val BACKGROUND_GRACE_PERIOD_MS = 2000L // 2 seconds grace period
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _session = MutableStateFlow<Session?>(null)
    val session: StateFlow<Session?> = _session.asStateFlow()
    
    private val _events = MutableSharedFlow<SessionEvent>()
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()
    
    private var backgroundedAt: Long? = null
    private var isInScanningFlow = false
    
    val currentSession: Session?
        get() = _session.value
    
    val hasActiveSession: Boolean
        get() = _session.value != null && _session.value?.status != SessionStatus.IDLE
    
    init {
        // Register lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    /**
     * Initialize and check for leftover temp files from previous sessions.
     * Called on app startup.
     */
    suspend fun initializeAndCleanup(): Boolean {
        val hadLeftovers = cleanupLeftoverTempFiles()
        _events.emit(SessionEvent.PreviousScanCleared(hadLeftovers))
        return hadLeftovers
    }
    
    /**
     * Start a new scanning session.
     */
    fun startSession(): Session {
        // Discard any existing session first
        discardCurrentSession()
        
        val tempDir = createSessionTempDir()
        val newSession = Session(
            id = UUID.randomUUID().toString(),
            status = SessionStatus.IDLE,
            tempDir = tempDir
        )
        
        _session.value = newSession
        isInScanningFlow = true
        
        Log.d(TAG, "Started new session: ${newSession.id}")
        return newSession
    }
    
    /**
     * Update the current session.
     */
    fun updateSession(update: (Session) -> Session) {
        _session.value?.let { current ->
            _session.value = update(current)
        }
    }
    
    /**
     * Update session status.
     */
    fun setStatus(status: SessionStatus) {
        updateSession { it.copy(status = status) }
    }
    
    /**
     * Set the source video for the session.
     */
    fun setSourceVideo(uri: String, durationMs: Long, isImported: Boolean) {
        updateSession { 
            it.copy(
                sourceVideoUri = uri,
                videoDurationMs = durationMs,
                isImportedVideo = isImported,
                trimRange = TrimRange(0L, durationMs)
            )
        }
    }
    
    /**
     * Set trim range.
     */
    fun setTrimRange(startMs: Long, endMs: Long) {
        updateSession { it.copy(trimRange = TrimRange(startMs, endMs)) }
    }
    
    /**
     * Add a manually selected page.
     */
    fun addSelectedPage(page: SelectedPage) {
        updateSession { session ->
            val newPages = session.selectedPages + page
            session.copy(
                selectedPages = newPages,
                pageOrder = newPages.map { it.id }
            )
        }
    }
    
    /**
     * Remove a manually selected page.
     */
    fun removeSelectedPage(pageId: String) {
        updateSession { session ->
            val newPages = session.selectedPages.filter { it.id != pageId }
            session.copy(
                selectedPages = newPages,
                pageOrder = newPages.map { it.id }
            )
        }
    }
    
    /**
     * Reorder selected pages.
     */
    fun reorderSelectedPages(fromIndex: Int, toIndex: Int) {
        updateSession { session ->
            val pages = session.selectedPages.toMutableList()
            if (fromIndex in pages.indices && toIndex in pages.indices) {
                val page = pages.removeAt(fromIndex)
                pages.add(toIndex, page)
            }
            session.copy(
                selectedPages = pages,
                pageOrder = pages.map { it.id }
            )
        }
    }
    
    /**
     * Set detected moments from auto-detection.
     * @deprecated Use addSelectedPage for manual selection
     */
    @Deprecated("Use addSelectedPage for manual selection edition")
    fun setDetectedMoments(
        moments: List<DetectedMoment>,
        excluded: List<ExcludedMoment> = emptyList()
    ) {
        updateSession { 
            it.copy(
                detectedMoments = moments,
                excludedMoments = excluded,
                selectedMomentIds = moments.filter { m -> m.isSelected }.map { m -> m.id }.toSet(),
                pageOrder = moments.filter { m -> m.isSelected }.map { m -> m.id }
            )
        }
    }
    
    /**
     * Toggle selection of a moment.
     * @deprecated Use addSelectedPage/removeSelectedPage for manual selection
     */
    @Deprecated("Use addSelectedPage/removeSelectedPage for manual selection edition")
    fun toggleMomentSelection(momentId: String) {
        updateSession { session ->
            val newSelected = if (momentId in session.selectedMomentIds) {
                session.selectedMomentIds - momentId
            } else {
                session.selectedMomentIds + momentId
            }
            val newOrder = if (momentId in session.selectedMomentIds) {
                session.pageOrder - momentId
            } else {
                session.pageOrder + momentId
            }
            session.copy(
                selectedMomentIds = newSelected,
                pageOrder = newOrder
            )
        }
    }
    
    /**
     * Update page order.
     */
    fun setPageOrder(order: List<String>) {
        updateSession { it.copy(pageOrder = order) }
    }
    
    /**
     * Apply edit to a page.
     */
    fun applyPageEdit(momentId: String, edit: PageEdit) {
        updateSession { 
            it.copy(pageEdits = it.pageEdits + (momentId to edit))
        }
    }
    
    /**
     * Update processing progress.
     */
    fun updateProcessingProgress(stage: ProcessingStage, progress: Float) {
        updateSession { 
            it.copy(
                currentProcessingStage = stage,
                processingProgress = progress
            )
        }
    }
    
    /**
     * Add processed image.
     */
    fun addProcessedImage(momentId: String, file: File) {
        updateSession { 
            it.copy(processedImages = it.processedImages + (momentId to file))
        }
    }
    
    /**
     * Set export result.
     */
    fun setExportResult(pdfPath: String, fileSize: Long) {
        updateSession { 
            it.copy(
                exportedPdfPath = pdfPath,
                exportedFileSize = fileSize,
                status = SessionStatus.EXPORT_RESULT
            )
        }
    }
    
    /**
     * Mark that we're entering the scanning flow (for background detection).
     */
    fun enterScanningFlow() {
        isInScanningFlow = true
    }
    
    /**
     * Mark that we're exiting the scanning flow normally (e.g., export complete).
     */
    fun exitScanningFlow() {
        isInScanningFlow = false
    }
    
    /**
     * Discard current session and cleanup temp files.
     * Called on back navigation, backgrounding, or explicit cancel.
     */
    fun discardCurrentSession() {
        _session.value?.let { session ->
            Log.d(TAG, "Discarding session: ${session.id}")
            
            // Clean up temp directory
            session.tempDir?.let { dir ->
                scope.launch(Dispatchers.IO) {
                    deleteRecursively(dir)
                }
            }
            
            // Emit event
            scope.launch {
                _events.emit(SessionEvent.SessionDiscarded)
            }
        }
        
        _session.value = null
        isInScanningFlow = false
    }
    
    /**
     * Complete session after successful export.
     * Cleans up temp files but keeps export metadata.
     */
    fun completeSession() {
        _session.value?.let { session ->
            Log.d(TAG, "Completing session: ${session.id}")
            
            // Clean up temp directory (intermediates)
            session.tempDir?.let { dir ->
                scope.launch(Dispatchers.IO) {
                    deleteRecursively(dir)
                }
            }
        }
        
        _session.value = null
        isInScanningFlow = false
    }
    
    // Lifecycle callbacks for background detection
    
    override fun onStart(owner: LifecycleOwner) {
        // App came to foreground
        backgroundedAt?.let { bgTime ->
            val elapsed = System.currentTimeMillis() - bgTime
            if (elapsed > BACKGROUND_GRACE_PERIOD_MS && isInScanningFlow && hasActiveSession) {
                Log.d(TAG, "App was backgrounded for ${elapsed}ms, discarding session")
                discardCurrentSession()
            }
        }
        backgroundedAt = null
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App went to background
        if (isInScanningFlow && hasActiveSession) {
            backgroundedAt = System.currentTimeMillis()
            Log.d(TAG, "App backgrounded with active session")
        }
    }
    
    // Private helpers
    
    private fun createSessionTempDir(): File {
        val baseDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (!baseDir.exists()) baseDir.mkdirs()
        
        val sessionDir = File(baseDir, UUID.randomUUID().toString())
        sessionDir.mkdirs()
        return sessionDir
    }
    
    private fun cleanupLeftoverTempFiles(): Boolean {
        val baseDir = File(context.cacheDir, TEMP_DIR_NAME)
        if (!baseDir.exists()) return false
        
        val children = baseDir.listFiles() ?: return false
        if (children.isEmpty()) return false
        
        Log.d(TAG, "Cleaning up ${children.size} leftover session directories")
        
        children.forEach { dir ->
            deleteRecursively(dir)
        }
        
        return true
    }
    
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        file.delete()
    }
}

