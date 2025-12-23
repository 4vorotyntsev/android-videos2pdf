package com.vs.videoscanpdf.ui.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vs.videoscanpdf.data.entities.FilterType

enum class EditorFilter(val displayName: String, val filterType: FilterType) {
    ORIGINAL("Original", FilterType.ORIGINAL),
    GRAYSCALE("B&W", FilterType.GRAYSCALE),
    ENHANCED("Document", FilterType.ENHANCED)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinglePageEditorScreen(
    projectId: String,
    pageId: String,
    onSave: () -> Unit,
    onBack: () -> Unit,
    viewModel: SinglePageEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(projectId, pageId) {
        viewModel.initialize(projectId, pageId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Page") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveChanges()
                            onSave()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // Image preview with zoom and pan
                    ZoomableImage(
                        bitmap = uiState.bitmap,
                        rotation = uiState.rotation.toFloat(),
                        filter = uiState.currentFilter,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                    
                    // Tool strip
                    ToolStrip(
                        currentFilter = uiState.currentFilter,
                        onRotate = viewModel::rotateRight,
                        onFilterSelect = viewModel::setFilter,
                        onReset = viewModel::reset
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    bitmap: Bitmap?,
    rotation: Float,
    filter: EditorFilter,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            val colorFilter = when (filter) {
                EditorFilter.GRAYSCALE -> {
                    // Grayscale matrix using luminance values
                    val matrix = ColorMatrix(
                        floatArrayOf(
                            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                            0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    ColorFilter.colorMatrix(matrix)
                }
                EditorFilter.ENHANCED -> {
                    // Enhanced contrast matrix
                    val matrix = ColorMatrix(
                        floatArrayOf(
                            1.2f, 0f, 0f, 0f, 0f,
                            0f, 1.2f, 0f, 0f, 0f,
                            0f, 0f, 1.2f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    ColorFilter.colorMatrix(matrix)
                }
                else -> null
            }
            
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Page image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        rotationZ = rotation
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 4f)
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-500f, 500f),
                                y = (offset.y + pan.y).coerceIn(-500f, 500f)
                            )
                        }
                    },
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter
            )
        } ?: CircularProgressIndicator()
    }
}

@Composable
private fun ToolStrip(
    currentFilter: EditorFilter,
    onRotate: () -> Unit,
    onFilterSelect: (EditorFilter) -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Main tools row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ToolButton(
                    icon = Icons.Default.RotateRight,
                    label = "Rotate",
                    onClick = onRotate
                )
                
                ToolButton(
                    icon = Icons.Default.Refresh,
                    label = "Reset",
                    onClick = onReset
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filters
            Text(
                text = "Filters",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onFilterSelect(filter) },
                        label = { Text(filter.displayName) },
                        leadingIcon = if (currentFilter == filter) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

