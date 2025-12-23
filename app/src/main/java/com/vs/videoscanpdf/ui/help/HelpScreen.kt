package com.vs.videoscanpdf.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vs.videoscanpdf.ui.components.SoftCard
import com.vs.videoscanpdf.ui.components.SoftCardSubtle

/**
 * Help screen with FAQ-style topics and privacy notice.
 * 
 * Features:
 * - FAQ-style collapsible list
 * - Privacy statement
 * - Settings link
 * - No OCR mentions (P0.5)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onSettingsClick: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Help") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // FAQ Section
            Text(
                text = "Frequently Asked Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            helpTopics.forEach { topic ->
                HelpItemCard(
                    question = topic.question,
                    answer = topic.answer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Privacy notice
            PrivacyNotice()
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HelpItemCard(
    question: String,
    answer: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    SoftCardSubtle(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        contentPadding = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun PrivacyNotice() {
    SoftCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "Your Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• All processing happens on your device\n" +
                           "• Your documents never leave your phone\n" +
                           "• No data is sent to any server\n" +
                           "• PDFs are saved to your chosen folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3
                )
            }
        }
    }
}

private data class HelpTopic(
    val question: String,
    val answer: String
)

private val helpTopics = listOf(
    HelpTopic(
        question = "How do I scan documents?",
        answer = "Tap 'Start scanning' on the home screen, then record a video of your pages. " +
                "Hold each page steady for about 1 second. We'll automatically detect the pages and create a PDF."
    ),
    HelpTopic(
        question = "What's the best way to record?",
        answer = "• Use good lighting - natural light works best\n" +
                "• Hold your phone steady\n" +
                "• Pause briefly (~1 second) on each page\n" +
                "• Keep the page fully visible in the frame\n" +
                "• Avoid shadows and glare"
    ),
    HelpTopic(
        question = "Can I import an existing video?",
        answer = "Yes! Tap 'Import video' on the home screen to select a video from your gallery. " +
                "The video must be at least 2 seconds long."
    ),
    HelpTopic(
        question = "How do I trim my video?",
        answer = "After recording or importing, you can trim the start and end to remove " +
                "any shaky parts. Drag the handles on the timeline to adjust."
    ),
    HelpTopic(
        question = "Where are my PDFs saved?",
        answer = "By default, PDFs are saved to your Documents folder. You can change this " +
                "location in Settings. All your exported PDFs appear in the Exports tab."
    ),
    HelpTopic(
        question = "What quality options are available?",
        answer = "• Email-friendly: Smaller file size, good for sharing\n" +
                "• Balanced: Good quality and reasonable size (default)\n" +
                "• Print: Highest quality for printing"
    ),
    HelpTopic(
        question = "Why did my scan disappear?",
        answer = "This app uses a 'no drafts' approach - if you leave the scanning flow " +
                "before exporting, the scan is discarded. This keeps your device clean " +
                "and your data private."
    ),
    HelpTopic(
        question = "Does this app work offline?",
        answer = "Yes! Everything happens on your device. No internet connection is needed " +
                "for scanning, processing, or saving PDFs."
    )
)
