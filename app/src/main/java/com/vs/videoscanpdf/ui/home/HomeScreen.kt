package com.vs.videoscanpdf.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.data.entities.ExportEntity
import com.vs.videoscanpdf.ui.components.CenteredTrustChips
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.SoftCard
import com.vs.videoscanpdf.ui.components.SoftCardSubtle
import com.vs.videoscanpdf.ui.permission.PermissionSheet
import com.vs.videoscanpdf.ui.permission.PermissionSheetState
import com.vs.videoscanpdf.ui.permission.PermissionType
import com.vs.videoscanpdf.ui.theme.AppTextStyles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen - SaaS landing inside the app.
 * 
 * Features:
 * - Hero section with promise and primary CTA
 * - Trust chips row ("Works offline", "Saved to your phone")
 * - Recent exports (limited to 3)
 * - Permission handling via bottom sheet (not on screen open)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartScanning: (sessionId: String) -> Unit,
    onImportVideo: (sessionId: String) -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: (sessionId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) {
            val sessionId = viewModel.startNewSession()
            onStartScanning(sessionId)
        }
    }
    
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        viewModel.dismissPermissionSheet()
        // After permission granted, user needs to tap import again
    }
    
    // Video picker
    var pendingVideoImport by remember { mutableStateOf(false) }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { videoUri ->
            val sessionId = viewModel.startNewSession()
            // Note: In real implementation, we'd pass the video URI to the import screen
            onImportVideo(sessionId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Video to PDF",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero section
            HeroSection(
                onStartScanning = {
                    if (viewModel.hasCameraPermission()) {
                        val sessionId = viewModel.startNewSession()
                        onStartScanning(sessionId)
                    } else {
                        viewModel.showCameraPermissionSheet()
                    }
                },
                onImportVideo = {
                    if (viewModel.hasMediaPermission()) {
                        videoPickerLauncher.launch("video/*")
                    } else {
                        viewModel.showMediaPermissionSheet()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recent exports section
            if (uiState.recentExports.isNotEmpty()) {
                RecentExportsSection(
                    exports = uiState.recentExports,
                    onExportClick = { export ->
                        // Navigate to view export
                        onExportClick(export.id)
                    },
                    onDeleteExport = { export ->
                        viewModel.deleteExport(export)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Camera permission sheet
    PermissionSheet(
        state = PermissionSheetState(
            isVisible = uiState.showCameraPermissionSheet,
            type = PermissionType.CAMERA
        ),
        onAllow = {
            viewModel.dismissPermissionSheet()
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onDismiss = { viewModel.dismissPermissionSheet() },
        onOpenSettings = { viewModel.dismissPermissionSheet() }
    )
    
    // Media permission sheet
    PermissionSheet(
        state = PermissionSheetState(
            isVisible = uiState.showMediaPermissionSheet,
            type = PermissionType.MEDIA
        ),
        onAllow = {
            viewModel.dismissPermissionSheet()
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            mediaPermissionLauncher.launch(permission)
        },
        onDismiss = { viewModel.dismissPermissionSheet() },
        onOpenSettings = { viewModel.dismissPermissionSheet() }
    )
}

/**
 * Hero section with main value proposition and CTA.
 */
@Composable
private fun HeroSection(
    onStartScanning: () -> Unit,
    onImportVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Hero headline
        Text(
            text = "Turn videos into clean PDFs",
            style = AppTextStyles.heroTitle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Hero subtext (friendly copy)
        Text(
            text = "Record pages as a video. We'll clean them up and save a readable PDF.",
            style = AppTextStyles.heroSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Trust chips
        CenteredTrustChips()
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Primary CTA (full width pill)
        PillButton(
            text = "Start scanning",
            onClick = onStartScanning,
            icon = Icons.Default.Videocam
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Secondary action
        TextButton(
            onClick = onImportVideo,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Import video",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Recent exports section with card list.
 */
@Composable
private fun RecentExportsSection(
    exports: List<ExportEntity>,
    onExportClick: (ExportEntity) -> Unit,
    onDeleteExport: (ExportEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Recent",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        exports.forEach { export ->
            ExportCard(
                export = export,
                onClick = { onExportClick(export) },
                onDelete = { onDeleteExport(export) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Export card showing PDF info with menu.
 */
@Composable
private fun ExportCard(
    export: ExportEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }
    
    SoftCardSubtle(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = export.pdfPath.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${export.pageCount} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(export.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
