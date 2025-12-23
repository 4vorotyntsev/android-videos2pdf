package com.vs.videoscanpdf.ui.recorder

import android.Manifest
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.SoftCard
import com.vs.videoscanpdf.ui.theme.AppTextStyles
import com.vs.videoscanpdf.ui.theme.RecordingRed
import com.vs.videoscanpdf.ui.theme.Success
import com.vs.videoscanpdf.ui.theme.Warning
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Camera recording screen with stability indicator.
 * 
 * Features:
 * - Fullscreen camera preview
 * - Page frame overlay
 * - Top hint: "Hold ~1s per page"
 * - Stability indicator (gyro-based)
 * - Recording timer
 * - Flash toggle
 * - "Looks good?" review sheet after recording
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    sessionId: String,
    onRecordingComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    // Handle back press - show discard dialog if recording started
    BackHandler {
        if (uiState.isRecording || uiState.showReviewButtons) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    
    // Initialize
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId)
    }
    
    // Navigate when complete
    LaunchedEffect(uiState.recordingComplete) {
        if (uiState.recordingComplete) {
            onRecordingComplete()
        }
    }
    
    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result handled
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        if (viewModel.hasCameraPermission()) {
            CameraPreview(
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        
        // Page frame overlay
        PageFrameOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        )
        
        // Top controls and hints
        TopControls(
            isRecording = uiState.isRecording,
            stabilityState = uiState.stabilityState,
            isTorchEnabled = uiState.isTorchEnabled,
            onTorchToggle = viewModel::toggleTorch,
            onBack = { 
                if (uiState.isRecording || uiState.showReviewButtons) {
                    showDiscardDialog = true
                } else {
                    onBack()
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
        
        // Bottom controls
        BottomControls(
            isRecording = uiState.isRecording,
            recordingDurationMs = uiState.recordingDurationMs,
            zoomLevel = uiState.zoomLevel,
            onStartRecording = viewModel::startRecording,
            onStopRecording = viewModel::stopRecording,
            onZoomChange = viewModel::setZoom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
        
        // Low storage warning
        if (uiState.hasLowStorage) {
            LowStorageWarning(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
            )
        }
    }
    
    // Review bottom sheet
    if (uiState.showReviewButtons) {
        ReviewBottomSheet(
            videoPath = uiState.recordedVideoPath,
            durationMs = uiState.recordingDurationMs,
            onUseVideo = viewModel::useVideo,
            onRetake = viewModel::retake
        )
    }
    
    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard recording?") },
            text = { Text("Your recorded video will be discarded.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.retake()
                        onBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep recording")
                }
            }
        )
    }
    
    // Error dialog
    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun CameraPreview(
    viewModel: RecorderViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = modifier,
        update = { previewView ->
            viewModel.setupCamera(
                previewView = previewView,
                lifecycleOwner = lifecycleOwner,
                executor = ContextCompat.getMainExecutor(context)
            )
        }
    )
}

@Composable
private fun PageFrameOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
    )
}

@Composable
private fun TopControls(
    isRecording: Boolean,
    stabilityState: StabilityState,
    isTorchEnabled: Boolean,
    onTorchToggle: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // Hint chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Hold ~1s per page",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
            
            IconButton(
                onClick = onTorchToggle,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = if (isTorchEnabled) Color.Yellow else Color.White
                )
            }
        }
        
        // Stability indicator (only when recording)
        if (isRecording) {
            StabilityIndicator(
                state = stabilityState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun StabilityIndicator(
    state: StabilityState,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            StabilityState.STABLE -> Success.copy(alpha = 0.8f)
            StabilityState.SLIGHTLY_SHAKY -> Warning.copy(alpha = 0.8f)
            StabilityState.TOO_SHAKY -> RecordingRed.copy(alpha = 0.8f)
        },
        label = "stability_bg"
    )
    
    val icon = when (state) {
        StabilityState.STABLE -> Icons.Default.Check
        StabilityState.SLIGHTLY_SHAKY -> Icons.Default.Warning
        StabilityState.TOO_SHAKY -> Icons.Default.Warning
    }
    
    val text = when (state) {
        StabilityState.STABLE -> "Stable"
        StabilityState.SLIGHTLY_SHAKY -> "Slightly shaky"
        StabilityState.TOO_SHAKY -> "Too shaky"
    }
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun BottomControls(
    isRecording: Boolean,
    recordingDurationMs: Long,
    zoomLevel: Float,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording duration
        if (isRecording) {
            Text(
                text = formatDuration(recordingDurationMs),
                style = AppTextStyles.timer,
                color = RecordingRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Zoom slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "1x", color = Color.White, style = MaterialTheme.typography.bodySmall)
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(text = "10x", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Record button
        RecordButton(
            isRecording = isRecording,
            onToggle = {
                if (isRecording) onStopRecording() else onStartRecording()
            }
        )
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isRecording) 32.dp else 64.dp)
                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                .background(RecordingRed)
                .then(
                    Modifier.size(if (isRecording) 32.dp else 64.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxSize()
            ) {
                // Empty - the colored background IS the button
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewBottomSheet(
    videoPath: String?,
    durationMs: Long,
    onUseVideo: () -> Unit,
    onRetake: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    // Get video thumbnail
    var thumbnail by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(videoPath) {
        videoPath?.let { path ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
                thumbnail = retriever.getFrameAtTime(0)
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = { /* Prevent dismiss */ },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Looks good?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Video thumbnail
            SoftCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                cornerRadius = 16.dp,
                contentPadding = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    thumbnail?.let { bmp ->
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Video preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    
                    // Duration overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatDuration(durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            PillButton(
                text = "Use video",
                onClick = onUseVideo
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PillButton(
                text = "Retake",
                onClick = onRetake,
                variant = PillButtonVariant.TEXT,
                icon = Icons.Default.Refresh
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LowStorageWarning(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Low storage space",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}
