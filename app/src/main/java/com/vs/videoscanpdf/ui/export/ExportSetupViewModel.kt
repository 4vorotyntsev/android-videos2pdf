package com.vs.videoscanpdf.ui.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.repository.ProjectRepository
import com.vs.videoscanpdf.data.repository.SettingsRepository
import com.vs.videoscanpdf.data.session.SessionManager
import com.vs.videoscanpdf.data.session.SessionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * UI state for export setup screen.
 */
data class ExportSetupUiState(
    val pageCount: Int = 0,
    val selectedPreset: ExportPreset = ExportPreset.BALANCED,
    val filename: String = "",
    val saveLocationName: String = "Documents",
    val saveLocationPath: String = "",
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isExportComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExportSetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private var sessionId: String? = null
    
    private val _uiState = MutableStateFlow(ExportSetupUiState())
    val uiState: StateFlow<ExportSetupUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        this.sessionId = sessionId
        sessionManager.setStatus(SessionStatus.EXPORT_SETUP)
        
        viewModelScope.launch {
            val session = sessionManager.currentSession ?: return@launch
            val savePath = settingsRepository.getPdfSavePath().first()
            
            // Generate default filename
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val defaultFilename = "Scan_${dateFormat.format(Date())}"
            
            _uiState.value = _uiState.value.copy(
                pageCount = session.selectedMomentIds.size,
                filename = defaultFilename,
                saveLocationPath = savePath,
                saveLocationName = File(savePath).name
            )
        }
    }
    
    fun setPreset(preset: ExportPreset) {
        _uiState.value = _uiState.value.copy(selectedPreset = preset)
    }
    
    fun setFilename(filename: String) {
        _uiState.value = _uiState.value.copy(filename = filename)
    }
    
    fun changeSaveLocation() {
        // In real implementation, use SAF to pick folder
        // For now, just use default
    }
    
    fun exportPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isExporting = true,
                exportProgress = 0f,
                error = null
            )
            
            try {
                val session = sessionManager.currentSession ?: throw Exception("No session")
                val state = _uiState.value
                
                // Get quality based on preset
                val quality = when (state.selectedPreset) {
                    ExportPreset.EMAIL_FRIENDLY -> 60
                    ExportPreset.BALANCED -> 80
                    ExportPreset.PRINT -> 95
                }
                
                // Prepare save directory
                val saveDir = settingsRepository.ensurePdfSaveDirectory(state.saveLocationPath)
                val pdfFile = settingsRepository.getUniquePdfFilePath(saveDir, state.filename)
                
                // Generate PDF
                val result = withContext(Dispatchers.IO) {
                    generatePdf(
                        session = session,
                        outputFile = pdfFile,
                        quality = quality,
                        onProgress = { progress ->
                            _uiState.value = _uiState.value.copy(exportProgress = progress)
                        }
                    )
                }
                
                if (result) {
                    // Save export record
                    projectRepository.saveExport(
                        pdfPath = pdfFile.absolutePath,
                        pageCount = session.selectedMomentIds.size,
                        fileSize = pdfFile.length()
                    )
                    
                    // Update session with result
                    sessionManager.setExportResult(
                        pdfPath = pdfFile.absolutePath,
                        fileSize = pdfFile.length()
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        isExportComplete = true
                    )
                } else {
                    throw Exception("PDF generation failed")
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = e.message ?: "Export failed"
                )
            }
        }
    }
    
    private fun generatePdf(
        session: com.vs.videoscanpdf.data.session.Session,
        outputFile: File,
        quality: Int,
        onProgress: (Float) -> Unit
    ): Boolean {
        val selectedMoments = session.detectedMoments.filter { it.id in session.selectedMomentIds }
        if (selectedMoments.isEmpty()) return false
        
        val document = PdfDocument()
        
        try {
            val pageOrder = session.pageOrder.ifEmpty { selectedMoments.map { it.id } }
            val orderedMoments = pageOrder.mapNotNull { id ->
                selectedMoments.find { it.id == id }
            }
            
            orderedMoments.forEachIndexed { index, moment ->
                onProgress((index + 0.5f) / orderedMoments.size)
                
                moment.thumbnail?.let { bitmap ->
                    // Scale bitmap for PDF
                    val scaledBitmap = scaleBitmapForPdf(bitmap, quality)
                    
                    // Create page
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        scaledBitmap.width,
                        scaledBitmap.height,
                        index + 1
                    ).create()
                    
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                    document.finishPage(page)
                }
                
                onProgress((index + 1f) / orderedMoments.size)
            }
            
            // Write to file
            FileOutputStream(outputFile).use { out ->
                document.writeTo(out)
            }
            
            return true
            
        } finally {
            document.close()
        }
    }
    
    private fun scaleBitmapForPdf(bitmap: Bitmap, quality: Int): Bitmap {
        // Scale based on quality preset
        val scale = when {
            quality >= 90 -> 1.0f  // Print quality - full resolution
            quality >= 70 -> 0.75f // Balanced
            else -> 0.5f // Email-friendly
        }
        
        val newWidth = (bitmap.width * scale * 5).toInt() // Scale up for PDF
        val newHeight = (bitmap.height * scale * 5).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

