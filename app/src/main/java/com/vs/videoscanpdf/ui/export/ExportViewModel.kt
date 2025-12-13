package com.vs.videoscanpdf.ui.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.data.entities.PageEntity
import com.vs.videoscanpdf.data.repository.ProjectRepository
import com.vs.videoscanpdf.data.repository.SettingsRepository
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
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the Export screen.
 * Handles PDF generation and sharing.
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
    
    private lateinit var currentProjectId: String
    
    fun initialize(projectId: String) {
        currentProjectId = projectId
        loadPageCount()
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val useGrayscale = settingsRepository.getUseGrayscale().first()
            val pdfFileName = settingsRepository.getPdfFileName().first()
            _uiState.value = _uiState.value.copy(
                useGrayscale = useGrayscale,
                pdfFileName = pdfFileName
            )
        }
    }
    
    private fun loadPageCount() {
        viewModelScope.launch {
            val count = projectRepository.getPageCount(currentProjectId)
            _uiState.value = _uiState.value.copy(pageCount = count)
        }
    }
    
    fun setPageSize(pageSize: PageSize) {
        _uiState.value = _uiState.value.copy(pageSize = pageSize)
    }
    
    fun setQuality(quality: ExportQuality) {
        _uiState.value = _uiState.value.copy(quality = quality)
    }

    fun setUseGrayscale(useGrayscale: Boolean) {
        _uiState.value = _uiState.value.copy(useGrayscale = useGrayscale)
    }

    fun setPdfFileName(name: String) {
        _uiState.value = _uiState.value.copy(
            pdfFileName = name.ifBlank { "scan" },
            isEditingFileName = false
        )
    }

    fun startEditingFileName() {
        _uiState.value = _uiState.value.copy(isEditingFileName = true)
    }

    fun cancelEditingFileName() {
        _uiState.value = _uiState.value.copy(isEditingFileName = false)
    }
    
    fun exportPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportProgress = 0f)
            
            try {
                val pages = projectRepository.getPagesByProjectSync(currentProjectId)
                val pdfPath = generatePdf(pages)
                
                // Save export record
                val exportId = UUID.randomUUID().toString()
                val export = ExportEntity(
                    id = exportId,
                    projectId = currentProjectId,
                    pdfPath = pdfPath,
                    pageCount = pages.size,
                    settingsJson = """{"pageSize":"${_uiState.value.pageSize.name}","quality":"${_uiState.value.quality.name}","grayscale":${_uiState.value.useGrayscale}}"""
                )
                projectRepository.addExport(export)
                
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportedPdfPath = pdfPath,
                    exportProgress = 1f
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Export failed: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun generatePdf(pages: List<PageEntity>): String = withContext(Dispatchers.IO) {
        // Get settings
        val pdfSavePath = settingsRepository.getPdfSavePath().first()
        
        // Use local state for these
        val useGrayscale = _uiState.value.useGrayscale
        val pdfFileName = _uiState.value.pdfFileName
        
        // Get unique file path (adds _1, _2, etc. if file exists)
        val saveDirectory = settingsRepository.ensurePdfSaveDirectory(pdfSavePath)
        val outputFile = settingsRepository.getUniquePdfFilePath(saveDirectory, pdfFileName)
        outputFile.parentFile?.mkdirs()
        
        val pdfDocument = PdfDocument()
        val pageSize = _uiState.value.pageSize
        val quality = _uiState.value.quality
        
        pages.forEachIndexed { index, page ->
            // Load bitmap
            val bitmap = BitmapFactory.decodeFile(page.imagePath) ?: return@forEachIndexed
            
            // Apply rotation if needed
            val rotatedBitmap = if (page.rotation != 0) {
                rotateBitmap(bitmap, page.rotation.toFloat())
            } else {
                bitmap
            }
            
            // Determine page dimensions
            val (pdfWidth, pdfHeight) = when (pageSize) {
                PageSize.AUTO -> {
                    // Use bitmap aspect ratio with reasonable PDF size
                    val aspectRatio = rotatedBitmap.width.toFloat() / rotatedBitmap.height
                    if (aspectRatio > 1) {
                        Pair(842, (842 / aspectRatio).toInt())
                    } else {
                        Pair((595 * aspectRatio).toInt(), 595)
                    }
                }
                else -> Pair(pageSize.widthPt, pageSize.heightPt)
            }
            
            // Create PDF page
            val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
            val pdfPage = pdfDocument.startPage(pageInfo)
            
            // Draw bitmap scaled to fit page
            val canvas = pdfPage.canvas
            val scaleX = pdfWidth.toFloat() / rotatedBitmap.width
            val scaleY = pdfHeight.toFloat() / rotatedBitmap.height
            val scale = minOf(scaleX, scaleY)
            
            val scaledWidth = rotatedBitmap.width * scale
            val scaledHeight = rotatedBitmap.height * scale
            val left = (pdfWidth - scaledWidth) / 2
            val top = (pdfHeight - scaledHeight) / 2
            
            val matrix = Matrix()
            matrix.postScale(scale, scale)
            matrix.postTranslate(left, top)
            
            // Apply grayscale filter if enabled
            val paint = if (useGrayscale) {
                Paint().apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
            } else {
                null
            }
            
            canvas.drawBitmap(rotatedBitmap, matrix, paint)
            
            pdfDocument.finishPage(pdfPage)
            
            // Clean up rotated bitmap if we created one
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            
            // Update progress
            val progress = (index + 1).toFloat() / pages.size
            _uiState.value = _uiState.value.copy(exportProgress = progress)
        }
        
        // Write PDF to file
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        
        outputFile.absolutePath
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    fun sharePdf() {
        val pdfPath = _uiState.value.exportedPdfPath ?: return
        val file = File(pdfPath)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
