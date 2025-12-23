package com.vs.videoscanpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vs.videoscanpdf.ui.theme.AppTextStyles
import com.vs.videoscanpdf.ui.theme.TrustChipBackground
import com.vs.videoscanpdf.ui.theme.TrustChipIcon
import com.vs.videoscanpdf.ui.theme.TrustChipText

/**
 * Predefined trust chip types for common reassurance messages.
 */
enum class TrustChipType(
    val text: String,
    val icon: ImageVector
) {
    WORKS_OFFLINE(
        text = "Works offline",
        icon = Icons.Default.CloudOff
    ),
    SAVED_TO_PHONE(
        text = "Saved to your phone",
        icon = Icons.Default.PhoneAndroid
    ),
    PRIVATE_SECURE(
        text = "Private & secure",
        icon = Icons.Default.Shield
    ),
    LOCAL_PROCESSING(
        text = "Processed locally",
        icon = Icons.Default.Lock
    )
}

/**
 * aiCarousels-style trust chip showing reassurance messages.
 * 
 * Used to build user confidence with non-deceptive social proof cues like:
 * - "Works offline"
 * - "Saved to your phone"
 * - "Private & secure"
 * 
 * @param type Predefined trust chip type
 * @param modifier Modifier for the chip
 */
@Composable
fun TrustChip(
    type: TrustChipType,
    modifier: Modifier = Modifier
) {
    TrustChip(
        text = type.text,
        icon = type.icon,
        modifier = modifier
    )
}

/**
 * Custom trust chip with custom text and icon.
 * 
 * @param text Chip label
 * @param icon Leading icon
 * @param modifier Modifier for the chip
 * @param backgroundColor Background color
 * @param contentColor Text and icon color
 */
@Composable
fun TrustChip(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TrustChipBackground,
    contentColor: Color = TrustChipText,
    iconColor: Color = TrustChipIcon
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iconColor
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = AppTextStyles.trustChip,
            color = contentColor
        )
    }
}

/**
 * Row of trust chips for displaying multiple reassurance messages.
 * 
 * @param chips List of trust chip types to display
 * @param modifier Modifier for the row
 */
@Composable
fun TrustChipRow(
    chips: List<TrustChipType>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chipType ->
            TrustChip(type = chipType)
        }
    }
}

/**
 * Centered trust chip row commonly used in home/landing screens.
 */
@Composable
fun CenteredTrustChips(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrustChip(type = TrustChipType.WORKS_OFFLINE)
        TrustChip(type = TrustChipType.SAVED_TO_PHONE)
    }
}

