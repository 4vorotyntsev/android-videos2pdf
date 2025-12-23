package com.vs.videoscanpdf.ui.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: String,
    onExportComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.isExporting) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (uiState.exportedPdfPath != null) {
                // Export complete view
                ExportCompleteContent(
                    pdfPath = uiState.exportedPdfPath!!,
                    pageCount = uiState.pageCount,
                    fileSize = uiState.exportedFileSize,
                    firstPageBitmap = uiState.firstPageBitmap,
                    onShare = viewModel::sharePdf,
                    onOpen = viewModel::openPdf,
                    onSaveTo = viewModel::savePdfTo,
                    onCreateAnother = onExportComplete,
                    onDone = onExportComplete
                )
            } else {
                // Export options view
                ExportOptionsContent(
                    uiState = uiState,
                    onPageSizeChange = viewModel::setPageSize,
                    onQualityChange = viewModel::setQuality,
                    onGrayscaleChange = viewModel::setUseGrayscale,
                    onFileNameChange = viewModel::setPdfFileName,
                    onStartEditingFileName = viewModel::startEditingFileName,
                    onCancelEditingFileName = viewModel::cancelEditingFileName,
                    onExport = viewModel::exportPdf
                )
            }
        }
    }
}

@Composable
private fun ExportOptionsContent(
    uiState: ExportUiState,
    onPageSizeChange: (PageSize) -> Unit,
    onQualityChange: (ExportQuality) -> Unit,
    onGrayscaleChange: (Boolean) -> Unit,
    onFileNameChange: (String) -> Unit,
    onStartEditingFileName: () -> Unit,
    onCancelEditingFileName: () -> Unit,
    onExport: () -> Unit
) {
    Column {
        // Filename and Grayscale Settings
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Filename
                if (uiState.isEditingFileName) {
                    var editName by androidx.compose.runtime.remember(uiState.pdfFileName) { 
                        androidx.compose.runtime.mutableStateOf(uiState.pdfFileName) 
                    }
                    
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("File Name") },
                        suffix = { Text(".pdf") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onCancelEditingFileName) {
                            Text("Cancel")
                        }
                        TextButton(onClick = { onFileNameChange(editName) }) {
                            Text("Save")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "File Name",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.pdfFileName}.pdf",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        IconButton(onClick = onStartEditingFileName) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit name")
                        }
                    }
                }
                
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Grayscale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Black & White",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Convert pages to grayscale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.useGrayscale,
                        onCheckedChange = onGrayscaleChange
                    )
                }
            }
        }

        // Pages info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.pages_count, uiState.pageCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Page size selection
        Text(
            text = stringResource(R.string.page_size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            PageSize.entries.forEach { size ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.pageSize == size,
                            onClick = { onPageSizeChange(size) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.pageSize == size,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(size.displayName)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quality selection
        Text(
            text = stringResource(R.string.quality),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            ExportQuality.entries.forEach { quality ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = uiState.quality == quality,
                            onClick = { onQualityChange(quality) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = uiState.quality == quality,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(quality.displayName)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Export button with progress
        AnimatedVisibility(visible = uiState.isExporting) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { uiState.exportProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Generating PDF... ${(uiState.exportProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        Button(
            onClick = onExport,
            enabled = !uiState.isExporting && uiState.pageCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isExporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.generate_pdf))
        }
        
        // Error display
        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExportCompleteContent(
    pdfPath: String,
    pageCount: Int,
    fileSize: Long,
    firstPageBitmap: android.graphics.Bitmap?,
    onShare: () -> Unit,
    onOpen: () -> Unit,
    onSaveTo: () -> Unit,
    onCreateAnother: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success header
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.export_success),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Preview card with first page
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // First page thumbnail
                if (firstPageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = firstPageBitmap.asImageBitmap(),
                        contentDescription = "First page preview",
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // File details
                Text(
                    text = pdfPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "$pageCount pages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(fileSize),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Primary actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
            
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Secondary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSaveTo,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_to))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Create another button
        TextButton(
            onClick = onCreateAnother,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create another")
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
