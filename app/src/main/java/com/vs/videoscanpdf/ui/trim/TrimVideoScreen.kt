package com.vs.videoscanpdf.ui.trim

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.SoftCard
import com.vs.videoscanpdf.ui.theme.ScrubberPlayed
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Trim video screen with video editor style controls.
 * 
 * Features:
 * - Timeline with frame thumbnails strip
 * - Draggable start/end handles
 * - Playhead indicator
 * - Preview playback
 * - "Continue" primary CTA, "Skip" secondary
 * - Enforces minimum 2s after trim
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimVideoScreen(
    sessionId: String,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    viewModel: TrimVideoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Initialize
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId)
    }
    
    // ExoPlayer
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    LaunchedEffect(uiState.videoUri) {
        uiState.videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            exoPlayer.prepare()
        }
    }
    
    // Update position from player
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }
        }
        exoPlayer.addListener(listener)
    }
    
    // Position update loop
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                viewModel.setCurrentPosition(exoPlayer.currentPosition)
                
                // Loop within trim range
                if (exoPlayer.currentPosition >= uiState.trimEndMs) {
                    exoPlayer.seekTo(uiState.trimStartMs)
                }
            }
            kotlinx.coroutines.delay(100)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trim video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Error loading video",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
            ) {
                // Helper text
                Text(
                    text = "Trim the shaky parts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Drag the handles to keep only the pages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Video preview
                SoftCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    cornerRadius = 16.dp,
                    contentPadding = 0.dp
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Play/Pause overlay
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.seekTo(uiState.trimStartMs)
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Trim timeline
                TrimTimeline(
                    thumbnails = uiState.thumbnails,
                    startProgress = uiState.startProgress,
                    endProgress = uiState.endProgress,
                    currentProgress = uiState.currentProgress,
                    onStartChange = { viewModel.setTrimStart(it) },
                    onEndChange = { viewModel.setTrimEnd(it) },
                    onSeek = { progress ->
                        val seekMs = (progress * uiState.videoDurationMs).toLong()
                        exoPlayer.seekTo(seekMs)
                        viewModel.setCurrentPosition(seekMs)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Duration info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Start: ${formatDuration(uiState.trimStartMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Duration: ${formatDuration(uiState.trimDurationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isTrimValid) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "End: ${formatDuration(uiState.trimEndMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!uiState.isTrimValid) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Minimum duration is 2 seconds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Action buttons
                PillButton(
                    text = "Continue",
                    onClick = {
                        viewModel.confirmTrim()
                        onContinue()
                    },
                    enabled = uiState.isTrimValid
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                PillButton(
                    text = "Skip",
                    onClick = {
                        viewModel.skipTrim()
                        onSkip()
                    },
                    variant = PillButtonVariant.TEXT
                )
            }
        }
    }
}

@Composable
private fun TrimTimeline(
    thumbnails: List<android.graphics.Bitmap>,
    startProgress: Float,
    endProgress: Float,
    currentProgress: Float,
    onStartChange: (Float) -> Unit,
    onEndChange: (Float) -> Unit,
    onSeek: (Float) -> Unit
) {
    val density = LocalDensity.current
    var timelineWidth by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .onSizeChanged { timelineWidth = it.width.toFloat() }
    ) {
        // Thumbnail strip background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnails.isNotEmpty()) {
                thumbnails.forEach { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        // Dimmed regions (outside trim)
        // Left dim
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(startProgress)
                .background(Color.Black.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
        )
        
        // Right dim
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(1f - endProgress)
                .align(Alignment.CenterEnd)
                .background(Color.Black.copy(alpha = 0.6f))
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
        )
        
        // Selected region border
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(endProgress - startProgress)
                .offset {
                    IntOffset((startProgress * timelineWidth).roundToInt(), 0)
                }
                .background(
                    Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
        ) {
            // Top and bottom borders
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        
        // Start handle
        TrimHandle(
            progress = startProgress,
            timelineWidth = timelineWidth,
            onDrag = onStartChange,
            isStart = true
        )
        
        // End handle
        TrimHandle(
            progress = endProgress,
            timelineWidth = timelineWidth,
            onDrag = onEndChange,
            isStart = false
        )
        
        // Playhead
        if (currentProgress in startProgress..endProgress) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((currentProgress * timelineWidth).roundToInt() - 1, 0)
                    }
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun TrimHandle(
    progress: Float,
    timelineWidth: Float,
    onDrag: (Float) -> Unit,
    isStart: Boolean
) {
    val handleWidth = 20.dp
    val density = LocalDensity.current
    val handleWidthPx = with(density) { handleWidth.toPx() }
    
    Box(
        modifier = Modifier
            .offset {
                val offsetX = if (isStart) {
                    (progress * timelineWidth - handleWidthPx).roundToInt()
                } else {
                    (progress * timelineWidth).roundToInt()
                }
                IntOffset(offsetX.coerceAtLeast(0), 0)
            }
            .width(handleWidth)
            .fillMaxHeight()
            .background(
                MaterialTheme.colorScheme.primary,
                if (isStart) {
                    RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                } else {
                    RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                }
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    val newProgress = (progress + dragAmount / timelineWidth).coerceIn(0f, 1f)
                    onDrag(newProgress)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Handle grip lines
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

