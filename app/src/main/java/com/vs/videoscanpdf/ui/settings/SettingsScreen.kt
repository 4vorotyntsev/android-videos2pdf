package com.vs.videoscanpdf.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.R
import com.vs.videoscanpdf.data.entities.ExportEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    exportsViewModel: ExportsHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exports by exportsViewModel.exports.collectAsState()
    var showPathDialog by remember { mutableStateOf(false) }
    var showDeleteExportDialog by remember { mutableStateOf<ExportEntity?>(null) }
    var editPath by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // PDF Save Location Section
            item {
                SettingsSection(title = "PDF Save Location") {
                    SettingsClickableItem(
                        icon = Icons.Default.Folder,
                        title = "Save PDFs to",
                        subtitle = uiState.pdfSavePath.ifEmpty { "Not set" },
                        onClick = {
                            editPath = uiState.pdfSavePath
                            showPathDialog = true
                        }
                    )
                }
            }
            
            item { HorizontalDivider() }
            
            // PDF Export Settings Section
            item {
                SettingsSection(title = "PDF Export Settings") {
                    // PDF File Name
                    SettingsClickableItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "PDF File Name",
                        subtitle = "${uiState.pdfFileName}.pdf",
                        onClick = { viewModel.startEditingFileName() }
                    )
                    
                    // Grayscale Toggle
                    SettingsToggleItem(
                        icon = Icons.Default.Folder, // Using Folder as placeholder, ideally use a grayscale icon
                        title = "Export in Grayscale",
                        subtitle = "Convert pages to black and white in PDF",
                        checked = uiState.useGrayscale,
                        onCheckedChange = { viewModel.setUseGrayscale(it) }
                    )
                }
            }
            
            item { HorizontalDivider() }
            
            // Video Quality Section
            item {
                SettingsSection(title = stringResource(R.string.video_quality)) {
                    SettingsToggleItem(
                        icon = Icons.Default.HighQuality,
                        title = stringResource(R.string.video_quality_max),
                        subtitle = "Use highest available quality when recording",
                        checked = uiState.useMaxQuality,
                        onCheckedChange = { viewModel.setUseMaxQuality(it) }
                    )
                }
            }
            
            item { HorizontalDivider() }
            
            // Storage Section
            item {
                SettingsSection(title = "Storage") {
                    SettingsToggleItem(
                        icon = Icons.Default.Delete,
                        title = stringResource(R.string.delete_video_after_export),
                        subtitle = stringResource(R.string.delete_video_after_export_summary),
                        checked = uiState.deleteVideoAfterExport,
                        onCheckedChange = { viewModel.setDeleteVideoAfterExport(it) }
                    )
                }
            }
            
            item { HorizontalDivider() }
            
            // Export History Section
            item {
                SettingsSection(title = "Export History") {
                    if (exports.isEmpty()) {
                        Text(
                            text = "No exports yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            items(exports) { export ->
                ExportHistoryItem(
                    export = export,
                    onDelete = { showDeleteExportDialog = export }
                )
            }
            
            item { HorizontalDivider() }
            
            // Diagnostics Section
            item {
                SettingsSection(title = stringResource(R.string.diagnostics)) {
                    SettingsClickableItem(
                        icon = Icons.Default.BugReport,
                        title = stringResource(R.string.export_diagnostics),
                        subtitle = "Export usage data for troubleshooting",
                        onClick = {
                            // TODO: Implement diagnostics export in M5
                        }
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
            
            // App version
            item {
                Text(
                    text = "VideoScan PDF v1.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
    
    // Path edit dialog
    if (showPathDialog) {
        AlertDialog(
            onDismissRequest = { showPathDialog = false },
            title = { Text("PDF Save Location") },
            text = {
                Column {
                    Text(
                        text = "Enter the folder path where PDFs should be saved:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = editPath,
                        onValueChange = { editPath = it },
                        label = { Text("Folder path") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.resetToDefaultPath()
                            showPathDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Default")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setPdfSavePath(editPath)
                        showPathDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete export confirmation dialog
    showDeleteExportDialog?.let { export ->
        AlertDialog(
            onDismissRequest = { showDeleteExportDialog = null },
            title = { Text("Delete Export") },
            text = { Text("Are you sure you want to delete this export? The PDF file will also be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        exportsViewModel.deleteExport(export)
                        showDeleteExportDialog = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteExportDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // PDF file name edit dialog
    if (uiState.isEditingFileName) {
        var editFileName by remember { mutableStateOf(uiState.pdfFileName) }
        
        AlertDialog(
            onDismissRequest = { viewModel.cancelEditingFileName() },
            title = { Text("PDF File Name") },
            text = {
                Column {
                    Text(
                        text = "Enter the base name for exported PDFs:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "If a file with this name exists, a number will be added (e.g., scan_1.pdf)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = editFileName,
                        onValueChange = { editFileName = it },
                        label = { Text("File name") },
                        singleLine = true,
                        suffix = { Text(".pdf") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.resetToDefaultFileName()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Default (scan)")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setPdfFileName(editFileName)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelEditingFileName() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExportHistoryItem(
    export: ExportEntity,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${export.pageCount} pages",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = dateFormat.format(Date(export.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = export.pdfPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete export",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
