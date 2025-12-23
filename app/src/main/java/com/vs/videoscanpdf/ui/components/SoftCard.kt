package com.vs.videoscanpdf.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * aiCarousels-style soft card with rounded corners and subtle shadow.
 * 
 * Features:
 * - Generous corner radius (16-20dp)
 * - Soft, diffused shadow
 * - Optional border for outlined variant
 * 
 * @param modifier Modifier for the card
 * @param cornerRadius Corner radius
 * @param elevation Shadow elevation
 * @param backgroundColor Card background color
 * @param contentPadding Internal padding
 * @param content Card content
 */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 4.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Soft card variant with primary container background.
 * Used for highlighted information or success states.
 */
@Composable
fun SoftCardHighlight(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Soft card variant with surface variant background.
 * Used for secondary information or form sections.
 */
@Composable
fun SoftCardSubtle(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = 0.dp,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentPadding = contentPadding,
        onClick = onClick,
        content = content
    )
}

/**
 * Outlined soft card with border instead of shadow.
 */
@Composable
fun SoftCardOutlined(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: Dp = 16.dp,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

/**
 * Success state card with green tint.
 */
@Composable
fun SoftCardSuccess(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard(
        modifier = modifier,
        cornerRadius = cornerRadius,
        elevation = 0.dp,
        backgroundColor = com.vs.videoscanpdf.ui.theme.SuccessContainer,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * Reassurance panel card for displaying soft confirmation messages.
 * Used in Auto Moments screen: "Looks good — 18 pages found"
 */
@Composable
fun ReassuranceCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    SoftCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        elevation = 2.dp,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
        contentPadding = 20.dp,
        content = content
    )
}

