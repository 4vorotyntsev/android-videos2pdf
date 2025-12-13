package com.vs.videoscanpdf.ui.recorder

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.R
import com.vs.videoscanpdf.ui.theme.RecordingRed
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RecorderScreen(
    projectId: String,
    onRecordingComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { mutableStateOf(viewModel.hasCameraPermission()) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }
    
    // Initialize ViewModel with project ID
    LaunchedEffect(projectId) {
        viewModel.initialize(projectId)
    }
    
    // Request permission if needed
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Navigate on recording complete
    LaunchedEffect(uiState.recordingComplete) {
        if (uiState.recordingComplete) {
            onRecordingComplete()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            // Camera preview
            CameraPreview(
                viewModel = viewModel,
                lifecycleOwner = lifecycleOwner
            )
            
            // Top bar
            TopControls(
                onBack = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    }
                    onBack()
                },
                isTorchEnabled = uiState.isTorchEnabled,
                onTorchToggle = viewModel::toggleTorch,
                isRecording = uiState.isRecording
            )
            
            // Recording duration
            if (uiState.isRecording) {
                RecordingIndicator(
                    durationMs = uiState.recordingDurationMs,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp)
                )
            }
            
            // Bottom controls
            BottomControls(
                isRecording = uiState.isRecording,
                onRecordClick = {
                    if (uiState.isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                zoomLevel = uiState.zoomLevel,
                onZoomChange = viewModel::setZoom,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            // Guidance overlay
            AnimatedVisibility(
                visible = uiState.showGuidance && !uiState.isRecording,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                GuidanceCard(
                    onDismiss = viewModel::dismissGuidance
                )
            }
        } else {
            // Permission denied state
            PermissionDeniedContent(
                onBack = onBack
            )
        }
    }
}

@Composable
private fun CameraPreview(
    viewModel: RecorderViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                viewModel.setupCamera(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    executor = ContextCompat.getMainExecutor(ctx)
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun TopControls(
    onBack: () -> Unit,
    isTorchEnabled: Boolean,
    onTorchToggle: () -> Unit,
    isRecording: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        if (!isRecording) {
            IconButton(onClick = onTorchToggle) {
                Icon(
                    imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Toggle flash",
                    tint = if (isTorchEnabled) Color.Yellow else Color.White
                )
            }
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun RecordingIndicator(
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(RecordingRed, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatDuration(durationMs),
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomControls(
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom slider
        if (!isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("1x", color = Color.White, style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = zoomLevel,
                    onValueChange = onZoomChange,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                Text("10x", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Record button
        IconButton(
            onClick = onRecordClick,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isRecording) RecordingRed else Color.White)
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint = if (isRecording) Color.White else Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isRecording) 
                stringResource(R.string.tap_to_stop) 
            else 
                stringResource(R.string.tap_to_record),
            color = Color.White,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun GuidanceCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.guidance_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.guidance_text),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.camera_permission_denied),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onBack) {
            Text("Go Back")
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
