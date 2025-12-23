package com.vs.videoscanpdf.ui.processing

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.data.session.ProcessingStage
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.SoftCard
import com.vs.videoscanpdf.ui.theme.StageActive
import com.vs.videoscanpdf.ui.theme.StageComplete
import com.vs.videoscanpdf.ui.theme.StagePending

/**
 * Processing screen - shows progress while creating PDF.
 * 
 * Features:
 * - Title: "Making your PDF"
 * - Subtext: "Usually under a minute"
 * - Friendly step labels (not numbered)
 * - Live first-page preview ASAP
 * - "Run in background" option
 * - Cancel discards everything
 */
@Composable
fun ProcessingScreen(
    sessionId: String,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showCancelDialog by remember { mutableStateOf(false) }
    
    BackHandler {
        showCancelDialog = true
    }
    
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId)
    }
    
    // Navigate when complete
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Making your PDF",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Subtitle
            Text(
                text = "Usually under a minute",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Overall progress
            LinearProgressIndicator(
                progress = { uiState.overallProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(uiState.overallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Live preview card
            if (uiState.previewBitmap != null) {
                PreviewCard(
                    bitmap = uiState.previewBitmap!!,
                    pagesProcessed = uiState.pagesProcessed,
                    totalPages = uiState.totalPages
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Processing steps
            ProcessingSteps(currentStage = uiState.currentStage)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Background option
            PillButton(
                text = "Run in background",
                onClick = { viewModel.runInBackground() },
                variant = PillButtonVariant.SECONDARY
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cancel
            TextButton(
                onClick = { showCancelDialog = true }
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel processing?") },
            text = { Text("Your progress will be lost and all files will be deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelProcessing()
                        onCancel()
                    }
                ) {
                    Text("Cancel & Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
private fun PreviewCard(
    bitmap: Bitmap,
    pagesProcessed: Int,
    totalPages: Int
) {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress text
            Text(
                text = "Processing page $pagesProcessed of $totalPages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProcessingSteps(currentStage: ProcessingStage) {
    val steps = listOf(
        StepInfo(
            stage = ProcessingStage.READING_VIDEO,
            label = "Reading video",
            icon = Icons.Default.Movie
        ),
        StepInfo(
            stage = ProcessingStage.DETECTING_PAGES,
            label = "Finding pages",
            icon = Icons.Default.Description
        ),
        StepInfo(
            stage = ProcessingStage.ENHANCING_IMAGES,
            label = "Making readable",
            icon = Icons.Default.Image
        ),
        StepInfo(
            stage = ProcessingStage.GENERATING_PDF,
            label = "Creating PDF",
            icon = Icons.Default.PictureAsPdf
        )
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        steps.forEach { step ->
            ProcessingStepItem(
                label = step.label,
                icon = step.icon,
                state = when {
                    step.stage.ordinal < currentStage.ordinal -> StepState.COMPLETE
                    step.stage.ordinal == currentStage.ordinal -> StepState.ACTIVE
                    else -> StepState.PENDING
                }
            )
        }
    }
}

private data class StepInfo(
    val stage: ProcessingStage,
    val label: String,
    val icon: ImageVector
)

private enum class StepState {
    PENDING, ACTIVE, COMPLETE
}

@Composable
private fun ProcessingStepItem(
    label: String,
    icon: ImageVector,
    state: StepState
) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            StepState.COMPLETE -> StageComplete
            StepState.ACTIVE -> StageActive
            StepState.PENDING -> StagePending
        },
        label = "step_bg"
    )
    
    val textColor = when (state) {
        StepState.COMPLETE -> MaterialTheme.colorScheme.onSurface
        StepState.ACTIVE -> MaterialTheme.colorScheme.primary
        StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Step indicator
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            if (state == StepState.COMPLETE) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else if (state == StepState.ACTIVE) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (state == StepState.ACTIVE) FontWeight.Medium else FontWeight.Normal
        )
    }
}
