package com.vs.videoscanpdf.ui.pagepicker

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.vs.videoscanpdf.data.session.SelectedPage
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.theme.AppTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Manual Page Picker Screen - Core screen for manual page selection.
 * 
 * Features:
 * - Video player with scrubbing
 * - Add page button captures current frame
 * - Selected pages strip with thumbnails
 * - Step controls (0.5s/1s forward/back)
 * - Continue button when pages selected
 * 
 * Philosophy:
 * - "You're in control" - user picks exactly what they want
 * - iOS/Google Photos-like selection feel
 * - One dominant action: Add page → Continue (when pages exist)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualPagePickerScreen(
    sessionId: String,
    onContinue: () -> Unit,
    onReviewPages: () -> Unit,
    onBack: () -> Unit,
    viewModel: ManualPagePickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showTipsDialog by remember { mutableStateOf(false) }
    
    // Back handler with discard confirmation
    BackHandler {
        if (uiState.selectedCount > 0) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId)
    }
    
    // Show capture error as snackbar
    LaunchedEffect(uiState.captureError) {
        uiState.captureError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.dismissCaptureError()
        }
    }
    
    // ExoPlayer setup
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    LaunchedEffect(uiState.videoUri) {
        uiState.videoUri?.let { uri ->
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
            exoPlayer.prepare()
            exoPlayer.seekTo(uiState.trimStartMs)
        }
    }
    
    // Sync position with ViewModel
    LaunchedEffect(uiState.currentPositionMs) {
        if (!exoPlayer.isPlaying) {
            exoPlayer.seekTo(uiState.currentPositionMs)
        }
    }
    
    // Position update loop
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }
        }
        exoPlayer.addListener(listener)
        
        while (true) {
            if (exoPlayer.isPlaying) {
                viewModel.setCurrentPosition(exoPlayer.currentPosition)
                
                // Loop within trim range
                if (exoPlayer.currentPosition >= uiState.trimEndMs) {
                    exoPlayer.pause()
                    exoPlayer.seekTo(uiState.trimStartMs)
                }
            }
            delay(50) // 20 fps update
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Pick your pages",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (uiState.selectedCount > 0) {
                            showDiscardDialog = true
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showTipsDialog = true }) {
                        Icon(
                            Icons.Default.HelpOutline, 
                            contentDescription = "Tips",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingContent(modifier = Modifier.padding(paddingValues))
        } else if (uiState.error != null) {
            ErrorContent(
                error = uiState.error!!,
                onBack = onBack,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Video preview area
                VideoPreviewArea(
                    exoPlayer = exoPlayer,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = uiState.currentPositionMs,
                    onPlayPauseToggle = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                        } else {
                            if (exoPlayer.currentPosition >= uiState.trimEndMs) {
                                exoPlayer.seekTo(uiState.trimStartMs)
                            }
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                
                // Scrubber and controls
                ScrubberArea(
                    currentProgress = uiState.currentProgress,
                    currentPositionMs = uiState.currentPositionMs,
                    durationMs = uiState.effectiveDurationMs,
                    thumbnails = uiState.timelineThumbnails,
                    stepSizeMs = uiState.stepSizeMs,
                    onSeek = { progress ->
                        val duration = uiState.trimEndMs - uiState.trimStartMs
                        val newPosition = uiState.trimStartMs + (progress * duration).toLong()
                        exoPlayer.seekTo(newPosition)
                        viewModel.setCurrentPosition(newPosition)
                    },
                    onStepForward = {
                        val newPos = (uiState.currentPositionMs + uiState.stepSizeMs).coerceAtMost(uiState.trimEndMs)
                        exoPlayer.seekTo(newPos)
                        viewModel.setCurrentPosition(newPos)
                    },
                    onStepBackward = {
                        val newPos = (uiState.currentPositionMs - uiState.stepSizeMs).coerceAtLeast(uiState.trimStartMs)
                        exoPlayer.seekTo(newPos)
                        viewModel.setCurrentPosition(newPos)
                    },
                    onStepSizeChange = { viewModel.setStepSize(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selected pages strip
                if (uiState.selectedCount > 0) {
                    SelectedPagesStrip(
                        pages = uiState.selectedPages,
                        onRemovePage = viewModel::removePage,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Bottom CTA area
                BottomCtaArea(
                    selectedCount = uiState.selectedCount,
                    isCapturing = uiState.isCapturing,
                    onAddPage = { 
                        exoPlayer.pause()
                        viewModel.addPageAtCurrentPosition() 
                    },
                    onContinue = {
                        viewModel.confirmSelection()
                        onContinue()
                    },
                    onReviewPages = {
                        viewModel.confirmSelection()
                        onReviewPages()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                )
            }
        }
    }
    
    // Discard confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard pages?") },
            text = { Text("You have ${uiState.selectedCount} page${if (uiState.selectedCount > 1) "s" else ""} selected. Going back will discard your selection.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
    
    // Tips dialog
    if (showTipsDialog) {
        AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = { Text("Tips for best results") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TipItem("📌", "Pause before adding", "Paused frames are sharper")
                    TipItem("📄", "One page at a time", "Add when page fills the screen")
                    TipItem("⏩", "Use step controls", "Fine-tune to the clearest frame")
                    TipItem("🔄", "Pages can overlap", "We'll clean up duplicates later")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTipsDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun TipItem(emoji: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(emoji, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading video...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "This video can't be processed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            PillButton(
                text = "Pick another video",
                onClick = onBack
            )
        }
    }
}

@Composable
private fun VideoPreviewArea(
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    currentPositionMs: Long,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Video player
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onPlayPauseToggle)
        )
        
        // Play/pause overlay (shows when paused)
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onPlayPauseToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // Timestamp badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = formatDuration(currentPositionMs),
                style = AppTextStyles.timer,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ScrubberArea(
    currentProgress: Float,
    currentPositionMs: Long,
    durationMs: Long,
    thumbnails: List<Bitmap>,
    stepSizeMs: Long,
    onSeek: (Float) -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onStepSizeChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        // Timeline scrubber
        TimelineScrubber(
            progress = currentProgress,
            thumbnails = thumbnails,
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Step controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step backward
            StepButton(
                icon = Icons.Default.ChevronLeft,
                label = if (stepSizeMs >= 1000) "1s" else "0.5s",
                onClick = onStepBackward
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Step size toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StepSizeChip(
                    label = "0.5s",
                    isSelected = stepSizeMs == 500L,
                    onClick = { onStepSizeChange(500L) }
                )
                StepSizeChip(
                    label = "1s",
                    isSelected = stepSizeMs == 1000L,
                    onClick = { onStepSizeChange(1000L) }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Step forward
            StepButton(
                icon = Icons.Default.ChevronRight,
                label = if (stepSizeMs >= 1000) "1s" else "0.5s",
                onClick = onStepForward
            )
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun StepSizeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimelineScrubber(
    progress: Float,
    thumbnails: List<Bitmap>,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var scrubberWidth by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onSizeChanged { scrubberWidth = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / scrubberWidth).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / scrubberWidth).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        // Thumbnail strip
        if (thumbnails.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
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
        
        // Progress overlay
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        )
        
        // Playhead
        Box(
            modifier = Modifier
                .offset {
                    IntOffset((progress * scrubberWidth).roundToInt() - with(density) { 2.dp.toPx() }.toInt(), 0)
                }
                .width(4.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        
        // Playhead handle
        Box(
            modifier = Modifier
                .offset {
                    IntOffset((progress * scrubberWidth).roundToInt() - with(density) { 8.dp.toPx() }.toInt(), -with(density) { 4.dp.toPx() }.toInt())
                }
                .size(16.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun SelectedPagesStrip(
    pages: List<SelectedPage>,
    onRemovePage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected: ${pages.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Thumbnail strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = pages,
                key = { _, page -> page.id }
            ) { index, page ->
                SelectedPageThumbnail(
                    page = page,
                    pageNumber = index + 1,
                    onRemove = { onRemovePage(page.id) }
                )
            }
        }
    }
}

@Composable
private fun SelectedPageThumbnail(
    page: SelectedPage,
    pageNumber: Int,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
    ) {
        // Thumbnail
        page.thumbnail?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page $pageNumber",
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
        
        // Page number badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(2.dp)
                .size(18.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$pageNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun BottomCtaArea(
    selectedCount: Int,
    isCapturing: Boolean,
    onAddPage: () -> Unit,
    onContinue: () -> Unit,
    onReviewPages: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Philosophy: While selected==0, primary is "Add page"
        // While selected>0, primary is "Continue", secondary is "Add another page"
        
        if (selectedCount == 0) {
            // No pages yet - Add page is the dominant action
            PillButton(
                text = "Add page",
                onClick = onAddPage,
                loading = isCapturing,
                icon = Icons.Default.Add
            )
        } else {
            // Has pages - Continue is dominant, Add is secondary
            PillButton(
                text = "Continue",
                onClick = onContinue,
                enabled = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PillButton(
                    text = "Add page",
                    onClick = onAddPage,
                    loading = isCapturing,
                    variant = PillButtonVariant.SECONDARY,
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f)
                )
                
                PillButton(
                    text = "Review",
                    onClick = onReviewPages,
                    variant = PillButtonVariant.TEXT,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    val millis = (durationMs % 1000) / 100
    return String.format(Locale.getDefault(), "%d:%02d.%d", minutes, seconds, millis)
}

