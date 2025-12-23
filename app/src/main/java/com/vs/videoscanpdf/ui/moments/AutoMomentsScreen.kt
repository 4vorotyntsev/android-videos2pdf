package com.vs.videoscanpdf.ui.moments

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.data.session.DetectedMoment
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant
import com.vs.videoscanpdf.ui.components.ReassuranceCard
import com.vs.videoscanpdf.ui.components.SoftCardSubtle

/**
 * Auto Page Moments screen - shows detected pages with proposal.
 * 
 * Features:
 * - Soft reassurance panel: "Looks good - 18 pages found"
 * - Summary chips: "18 selected", "3 excluded"
 * - Grid of detected page thumbnails
 * - Primary CTA: "Create my PDF"
 * - Secondary: "Review pages"
 * - Collapsed advanced section
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoMomentsScreen(
    sessionId: String,
    onCreatePdf: () -> Unit,
    onReviewPages: () -> Unit,
    onBack: () -> Unit,
    viewModel: AutoMomentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(sessionId) {
        viewModel.initialize(sessionId)
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
        when {
            uiState.isAnalyzing -> {
                AnalyzingContent(
                    progress = uiState.analysisProgress,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            uiState.detectedMoments.isEmpty() && !uiState.isAnalyzing -> {
                EmptyContent(
                    onRetry = { viewModel.rerunDetection() },
                    onBack = onBack,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            
            else -> {
                ProposalContent(
                    uiState = uiState,
                    onToggleMoment = viewModel::toggleMomentSelection,
                    onCreatePdf = {
                        viewModel.confirmSelection()
                        onCreatePdf()
                    },
                    onReviewPages = {
                        viewModel.confirmSelection()
                        onReviewPages()
                    },
                    onToggleAdvanced = viewModel::toggleAdvancedSettings,
                    onDensityChange = viewModel::setExtractionDensity,
                    onRerunDetection = viewModel::rerunDetection,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun AnalyzingContent(
    progress: Float,
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
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Finding your pages...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "This usually takes a few seconds",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyContent(
    onRetry: () -> Unit,
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
                text = "No pages detected",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We couldn't find any clear pages in your video. Try recording again with better lighting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PillButton(
                text = "Try again",
                onClick = onRetry,
                icon = Icons.Default.Refresh
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PillButton(
                text = "Go back",
                onClick = onBack,
                variant = PillButtonVariant.TEXT
            )
        }
    }
}

@Composable
private fun ProposalContent(
    uiState: AutoMomentsUiState,
    onToggleMoment: (String) -> Unit,
    onCreatePdf: () -> Unit,
    onReviewPages: () -> Unit,
    onToggleAdvanced: () -> Unit,
    onDensityChange: (Float) -> Unit,
    onRerunDetection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reassurance panel
        ReassuranceCard {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "We found your pages",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Looks good — ${uiState.selectedCount} pages selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryChip(
                text = "${uiState.selectedCount} selected",
                isHighlighted = true
            )
            if (uiState.excludedCount > 0) {
                SummaryChip(
                    text = "${uiState.excludedCount} excluded",
                    isHighlighted = false
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Pages grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(uiState.detectedMoments) { moment ->
                PageTile(
                    moment = moment,
                    isSelected = moment.id in uiState.selectedMomentIds,
                    onClick = { onToggleMoment(moment.id) }
                )
            }
        }
        
        // Advanced settings (collapsible)
        AdvancedSettings(
            isExpanded = uiState.showAdvancedSettings,
            onToggle = onToggleAdvanced,
            density = uiState.extractionDensity,
            onDensityChange = onDensityChange,
            onRerun = onRerunDetection
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        PillButton(
            text = "Create my PDF",
            onClick = onCreatePdf,
            enabled = uiState.selectedCount > 0
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        PillButton(
            text = "Review pages",
            onClick = onReviewPages,
            variant = PillButtonVariant.TEXT,
            enabled = uiState.selectedCount > 0
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryChip(
    text: String,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isHighlighted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isHighlighted) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PageTile(
    moment: DetectedMoment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        moment.thumbnail?.let { bitmap ->
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
        if (isSelected) {
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
    density: Float,
    onDensityChange: (Float) -> Unit,
    onRerun: () -> Unit
) {
    SoftCardSubtle(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = 0.dp
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
                        text = "Detection sensitivity",
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
                            value = density,
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

