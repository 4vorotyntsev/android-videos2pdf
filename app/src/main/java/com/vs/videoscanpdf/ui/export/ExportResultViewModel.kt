package com.vs.videoscanpdf.ui.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vs.videoscanpdf.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * UI state for export result screen.
 */
data class ExportResultUiState(
    val pdfPath: String = "",
    val filename: String = "",
    val pageCount: Int = 0,
    val fileSize: Long = 0L,
    val saveLocation: String = "",
    val firstPageThumbnail: Bitmap? = null,
    val cleanupComplete: Boolean = false
) {
    val formattedFileSize: String
        get() = when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        }
}

@HiltViewModel
class ExportResultViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ExportResultUiState())
    val uiState: StateFlow<ExportResultUiState> = _uiState.asStateFlow()
    
    fun initialize(sessionId: String) {
        viewModelScope.launch {
            val session = sessionManager.currentSession ?: return@launch
            
            val pdfPath = session.exportedPdfPath ?: return@launch
            val pdfFile = File(pdfPath)
            
            _uiState.value = ExportResultUiState(
                pdfPath = pdfPath,
                filename = pdfFile.name,
                pageCount = session.selectedMomentIds.size,
                fileSize = session.exportedFileSize,
                saveLocation = pdfFile.parentFile?.name ?: "Documents",
                cleanupComplete = true
            )
            
            // Load first page thumbnail
            loadFirstPageThumbnail(pdfPath)
        }
    }
    
    private fun loadFirstPageThumbnail(pdfPath: String) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val file = File(pdfPath)
                    val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fileDescriptor)
                    
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val bitmap = Bitmap.createBitmap(
                            page.width * 2,
                            page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        fileDescriptor.close()
                        bitmap
                    } else {
                        renderer.close()
                        fileDescriptor.close()
                        null
                    }
                }
                
                bitmap?.let {
                    _uiState.value = _uiState.value.copy(firstPageThumbnail = it)
                }
            } catch (e: Exception) {
                // Ignore thumbnail loading errors
            }
        }
    }
    
    fun sharePdf(context: Context) {
        try {
            val file = File(_uiState.value.pdfPath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun openPdf(context: Context) {
        try {
            val file = File(_uiState.value.pdfPath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(openIntent)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun viewInFiles(context: Context) {
        try {
            val file = File(_uiState.value.pdfPath)
            val folderUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:${file.parentFile?.absolutePath?.removePrefix("/storage/emulated/0/")}")
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Fallback: open file manager
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open folder"))
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun completeSession() {
        sessionManager.completeSession()
    }
}

