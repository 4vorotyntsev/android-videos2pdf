package com.vs.videoscanpdf.ui.picker

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FramePickerScreen(
    projectId: String,
    importedVideoUri: String? = null,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    viewModel: FramePickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Initialize ViewModel with optional imported video
    LaunchedEffect(projectId, importedVideoUri) {
        viewModel.initialize(projectId, importedVideoUri)
    }
    
    // ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    
    // Setup player when video path is available
    LaunchedEffect(uiState.videoPath) {
        uiState.videoPath?.let { path ->
            val mediaItem = MediaItem.fromUri(Uri.parse(path))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }
    
    // Sync player position with UI state
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                viewModel.setPlaying(exoPlayer.isPlaying)
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }
        }
        exoPlayer.addListener(listener)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Scrubber position state
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    
    // Update slider position from exoPlayer
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.duration > 0) {
                sliderPosition = exoPlayer.currentPosition.toFloat() / exoPlayer.duration
                viewModel.seekTo(exoPlayer.currentPosition)
            }
            kotlinx.coroutines.delay(100)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Page Detection") },
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading video...")
                }
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header with detection status
                DetectionHeader(
                    detectedCount = uiState.detectedPages.count { it.isSelected },
                    totalDetected = uiState.detectedPages.size,
                    isDetecting = uiState.isAutoDetecting,
                    progress = uiState.autoDetectionProgress
                )
                
                // Video preview (smaller)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.isPlaying) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(12.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Timeline with detected page markers
                Timeline(
                    duration = uiState.videoDurationMs,
                    currentPosition = uiState.currentPositionMs,
                    detectedPages = uiState.detectedPages,
                    onSeek = { position ->
                        val seekMs = (position * uiState.videoDurationMs).toLong()
                        exoPlayer.seekTo(seekMs)
                        viewModel.seekTo(seekMs)
                    },
                    onPageClick = { timeMs ->
                        exoPlayer.seekTo(timeMs)
                        viewModel.seekTo(timeMs)
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Detected pages grid
                Text(
                    text = "Detected Pages",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.detectedPages) { page ->
                        DetectedPageTile(
                            page = page,
                            onClick = { viewModel.toggleDetectedPageSelection(page.timeMs) }
                        )
                    }
                }
                
                // Advanced settings (collapsible)
                AdvancedSettings(
                    isExpanded = uiState.showAdvancedSettings,
                    onToggle = viewModel::toggleAdvancedSettings,
                    extractionDensity = uiState.extractionDensity,
                    onDensityChange = viewModel::setExtractionDensity,
                    onRerun = viewModel::rerunAutoDetection
                )
                
                // Bottom action buttons
                BottomActions(
                    selectedCount = uiState.detectedPages.count { it.isSelected },
                    onGeneratePdf = {
                        viewModel.confirmDetectedPages()
                        onContinue()
                    },
                    onReviewPages = {
                        viewModel.confirmDetectedPages()
                        onContinue()
                    }
                )
            }
        }
    }
}

@Composable
private fun DetectionHeader(
    detectedCount: Int,
    totalDetected: Int,
    isDetecting: Boolean,
    progress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isDetecting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Detecting pages...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "$detectedCount pages selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (totalDetected > detectedCount) {
                            Text(
                                text = "${totalDetected - detectedCount} excluded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Timeline(
    duration: Long,
    currentPosition: Long,
    detectedPages: List<DetectedPage>,
    onSeek: (Float) -> Unit,
    onPageClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Timeline with markers
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            
            // Progress track
            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            
            // Page markers
            detectedPages.forEach { page ->
                val markerPosition = if (duration > 0) page.timeMs.toFloat() / duration else 0f
                Box(
                    modifier = Modifier
                        .offset(x = (markerPosition * 300).dp) // Approximate offset
                        .size(12.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(
                            if (page.isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onPageClick(page.timeMs) }
                )
            }
        }
        
        // Slider
        Slider(
            value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DetectedPageTile(
    page: DetectedPage,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (page.isSelected) 3.dp else 1.dp,
                color = if (page.isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        page.thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }
        
        // Selection indicator
        if (page.isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun AdvancedSettings(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    extractionDensity: Float,
    onDensityChange: (Float) -> Unit,
    onRerun: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column {
            // Toggle header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Extraction density",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fewer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = extractionDensity,
                            onValueChange = onDensityChange,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = onRerun,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-detect pages")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BottomActions(
    selectedCount: Int,
    onGeneratePdf: () -> Unit,
    onReviewPages: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = onGeneratePdf,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedCount > 0,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Generate PDF ($selectedCount pages)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(
            onClick = onReviewPages,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedCount > 0
        ) {
            Text("Review pages")
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
