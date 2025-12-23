package com.vs.videoscanpdf.ui.processing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

enum class ProcessingStep {
    READING_VIDEO,
    DETECTING_PAGES,
    ENHANCING_IMAGES,
    GENERATING_PDF,
    COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingScreen(
    projectId: String,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ProcessingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(projectId) {
        viewModel.startProcessing(projectId)
    }
    
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Processing") },
                actions = {
                    if (!uiState.isComplete) {
                        TextButton(onClick = { showCancelDialog = true }) {
                            Text("Cancel")
                        }
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
            Spacer(modifier = Modifier.height(32.dp))
            
            // Live preview
            AnimatedVisibility(
                visible = uiState.previewBitmap != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    uiState.previewBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Page preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // Placeholder when no preview
            if (uiState.previewBitmap == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Preparing...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Progress steps
            ProcessingSteps(
                currentStep = uiState.currentStep,
                progress = uiState.progress
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Overall progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (uiState.processedPages > 0) {
                    Text(
                        text = "${uiState.processedPages} of ${uiState.totalPages} pages processed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Run in background option
            if (!uiState.isComplete) {
                TextButton(
                    onClick = {
                        viewModel.runInBackground()
                        onCancel()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run in background")
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Processing?") },
            text = { Text("Are you sure you want to cancel? Progress will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelProcessing()
                        showCancelDialog = false
                        onCancel()
                    }
                ) {
                    Text("Yes, cancel", color = MaterialTheme.colorScheme.error)
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
private fun ProcessingSteps(
    currentStep: ProcessingStep,
    progress: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProcessingStepItem(
            icon = Icons.Default.VideoFile,
            title = "Reading video",
            isActive = currentStep == ProcessingStep.READING_VIDEO,
            isComplete = currentStep.ordinal > ProcessingStep.READING_VIDEO.ordinal,
            progress = if (currentStep == ProcessingStep.READING_VIDEO) progress else if (currentStep.ordinal > ProcessingStep.READING_VIDEO.ordinal) 1f else 0f
        )
        
        ProcessingStepItem(
            icon = Icons.Default.Description,
            title = "Detecting pages",
            isActive = currentStep == ProcessingStep.DETECTING_PAGES,
            isComplete = currentStep.ordinal > ProcessingStep.DETECTING_PAGES.ordinal,
            progress = if (currentStep == ProcessingStep.DETECTING_PAGES) progress else if (currentStep.ordinal > ProcessingStep.DETECTING_PAGES.ordinal) 1f else 0f
        )
        
        ProcessingStepItem(
            icon = Icons.Default.AutoAwesome,
            title = "Enhancing images",
            isActive = currentStep == ProcessingStep.ENHANCING_IMAGES,
            isComplete = currentStep.ordinal > ProcessingStep.ENHANCING_IMAGES.ordinal,
            progress = if (currentStep == ProcessingStep.ENHANCING_IMAGES) progress else if (currentStep.ordinal > ProcessingStep.ENHANCING_IMAGES.ordinal) 1f else 0f
        )
        
        ProcessingStepItem(
            icon = Icons.Default.Image,
            title = "Generating PDF",
            isActive = currentStep == ProcessingStep.GENERATING_PDF,
            isComplete = currentStep == ProcessingStep.COMPLETE,
            progress = if (currentStep == ProcessingStep.GENERATING_PDF) progress else if (currentStep == ProcessingStep.COMPLETE) 1f else 0f
        )
    }
}

@Composable
private fun ProcessingStepItem(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    isComplete: Boolean,
    progress: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    when {
                        isComplete -> MaterialTheme.colorScheme.primary
                        isActive -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isComplete -> MaterialTheme.colorScheme.primary
                    isActive -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (isActive && progress > 0f) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

