package com.vs.videoscanpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vs.videoscanpdf.ui.theme.AppTextStyles
import com.vs.videoscanpdf.ui.theme.PillGradientEnd
import com.vs.videoscanpdf.ui.theme.PillGradientStart
import com.vs.videoscanpdf.ui.theme.ShadowMedium

/**
 * aiCarousels-style pill button variants.
 */
enum class PillButtonVariant {
    PRIMARY,    // Solid purple, main CTA
    SECONDARY,  // Outlined, secondary action
    TEXT        // Text only, tertiary action
}

/**
 * aiCarousels-inspired pill button with rounded corners and soft shadow.
 * 
 * Primary CTA button with consistent styling across the app.
 * Features:
 * - Fully rounded pill shape
 * - Soft shadow for depth
 * - Optional leading icon
 * - Loading state support
 * 
 * @param text Button label
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param variant Button style variant
 * @param enabled Whether the button is enabled
 * @param loading Show loading indicator
 * @param icon Optional leading icon
 * @param fullWidth Whether to fill available width
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillButtonVariant = PillButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = true
) {
    val shape = RoundedCornerShape(percent = 50)
    val buttonModifier = modifier
        .height(56.dp)
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
    
    when (variant) {
        PillButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier
                    .shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                enabled = enabled && !loading,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                PillButtonContent(
                    text = text,
                    loading = loading,
                    icon = icon,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        
        PillButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shape,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
            ) {
                PillButtonContent(
                    text = text,
                    loading = loading,
                    icon = icon,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        PillButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = enabled && !loading,
                shape = shape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                PillButtonContent(
                    text = text,
                    loading = loading,
                    icon = icon,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PillButtonContent(
    text: String,
    loading: Boolean,
    icon: ImageVector?,
    contentColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Text(
            text = text,
            style = AppTextStyles.pillButton,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Sticky bottom CTA container for screens with primary action.
 * Provides consistent padding and background.
 */
@Composable
fun StickyBottomCta(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        content()
    }
}

