package com.vs.videoscanpdf.ui.permission

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vs.videoscanpdf.ui.components.PillButton
import com.vs.videoscanpdf.ui.components.PillButtonVariant

/**
 * Permission type for contextual permission request.
 */
enum class PermissionType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>
) {
    CAMERA(
        title = "Camera access",
        description = "We need camera access to record your document pages as a video.",
        icon = Icons.Default.CameraAlt,
        permissions = listOf(Manifest.permission.CAMERA)
    ),
    MEDIA(
        title = "Media access",
        description = "We need access to your photos and videos to import an existing video.",
        icon = Icons.Default.Folder,
        permissions = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_VIDEO
        )
    )
}

/**
 * State for permission sheet.
 */
data class PermissionSheetState(
    val isVisible: Boolean = false,
    val type: PermissionType = PermissionType.CAMERA,
    val isPermanentlyDenied: Boolean = false
)

/**
 * aiCarousels-style permission request bottom sheet.
 * 
 * Features:
 * - Contextual explanation of why permission is needed
 * - Non-scary, friendly copy
 * - "Allow" primary button, "Not now" secondary
 * - "Open Settings" option when permanently denied
 * 
 * @param state Sheet state
 * @param onAllow Called when user taps Allow
 * @param onDismiss Called when user dismisses
 * @param onOpenSettings Called when user wants to open settings (permanently denied case)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSheet(
    state: PermissionSheetState,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    if (state.isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = state.type.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Title
                Text(
                    text = state.type.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = if (state.isPermanentlyDenied) {
                        "Permission was denied. Please enable ${state.type.title.lowercase()} in your device settings to continue."
                    } else {
                        state.type.description
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action buttons
                if (state.isPermanentlyDenied) {
                    // Open Settings button
                    PillButton(
                        text = "Open Settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                            onOpenSettings()
                        },
                        icon = Icons.Default.Settings
                    )
                } else {
                    // Allow button
                    PillButton(
                        text = "Allow",
                        onClick = onAllow
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Secondary action
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Not now",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Simpler inline permission prompt (not a sheet).
 * Used for inline permission requests within screens.
 */
@Composable
fun PermissionPrompt(
    type: PermissionType,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = type.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = type.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PillButton(
            text = "Allow",
            onClick = onRequestPermission
        )
    }
}

