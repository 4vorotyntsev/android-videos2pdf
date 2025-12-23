package com.vs.videoscanpdf.ui.videoimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.SoftCard
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Import video screen - validates and previews imported video.
 * 
 * Features:
 * - System picker integration
 * - Video preview with thumbnail
 * - Codec validation
 * - Minimum duration check (2s)
 * - "Use video" / "Cancel" actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportVideoScreen(
    sessionId: String,
    onVideoSelected: () -> Unit,
    onBack: () -> Unit,
    viewModel: ImportVideoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.validateVideo(it) }
    }
    
    // Launch picker on first load if no video
    LaunchedEffect(Unit) {
        viewModel.initialize(sessionId)
        if (uiState.videoUri == null && !uiState.isValidating) {
            videoPickerLauncher.launch("video/*")
        }
    }
    
    // Navigate when video is confirmed
    LaunchedEffect(uiState.isConfirmed) {
        if (uiState.isConfirmed) {
            onVideoSelected()
        }
    }
    
    // ExoPlayer for preview
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    LaunchedEffect(uiState.videoUri) {
        uiState.videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isValidating -> {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Checking video...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    // Error state
                    ErrorContent(
                        error = uiState.error!!,
                        onPickAnother = { videoPickerLauncher.launch("video/*") },
                        onCancel = onBack
                    )
                }
                
                uiState.videoUri != null -> {
                    // Preview state
                    PreviewContent(
                        videoUri = uiState.videoUri!!,
                        durationMs = uiState.durationMs,
                        exoPlayer = exoPlayer,
                        onUseVideo = { viewModel.confirmVideo() },
                        onCancel = onBack
                    )
                }
                
                else -> {
                    // Empty state - waiting for picker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Select a video to import",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            PillButton(
                                text = "Choose video",
                                onClick = { videoPickerLauncher.launch("video/*") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    videoUri: Uri,
    durationMs: Long,
    exoPlayer: ExoPlayer,
    onUseVideo: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video preview
        SoftCard(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            cornerRadius = 16.dp,
            contentPadding = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Duration info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Duration: ${formatDuration(durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Action buttons
        PillButton(
            text = "Use video",
            onClick = onUseVideo
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PillButton(
            text = "Cancel",
            onClick = onCancel,
            variant = PillButtonVariant.TEXT
        )
    }
}

@Composable
private fun ErrorContent(
    error: ImportError,
    onPickAnother: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when (error) {
                ImportError.TOO_SHORT -> "Video is too short"
                ImportError.UNSUPPORTED_FORMAT -> "Unsupported video format"
                ImportError.CANNOT_READ -> "Cannot read video"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when (error) {
                ImportError.TOO_SHORT -> "Please select a video at least 2 seconds long."
                ImportError.UNSUPPORTED_FORMAT -> "Try a different video format (MP4, MOV, etc.)."
                ImportError.CANNOT_READ -> "The video file could not be accessed."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PillButton(
            text = "Pick another",
            onClick = onPickAnother
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PillButton(
            text = "Cancel",
            onClick = onCancel,
            variant = PillButtonVariant.TEXT
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

