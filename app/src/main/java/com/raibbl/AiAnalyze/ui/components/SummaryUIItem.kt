package com.raibbl.AiAnalyze.ui.components
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import com.raibbl.AiAnalyze.ui.theme.AppTheme
import com.raibbl.AiAnalyze.models.SummaryViewModel
import com.raibbl.AiAnalyze.models.SummaryViewModelFactory
import androidx.core.content.FileProvider
import com.raibbl.AiAnalyze.utils.buildShareSummaryIntent
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import com.raibbl.AiAnalyze.utils.openSummary
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SummaryUiItem(summaryItem: SummaryItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext as Application
    val factory = remember { SummaryViewModelFactory(applicationContext) }
    val viewModel: SummaryViewModel = viewModel(factory = factory)
    val formattedTime = rememberFormattedTimestamp(summaryItem.timestamp)
    var expanded by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    val allTags by viewModel.allTags.observeAsState(emptyList())
    val dismissState = rememberDismissState(
        confirmStateChange = {
            if (it == DismissValue.DismissedToStart) {
                viewModel.deleteById(summaryItem.id)
                true
            } else false
        }
    )


    SwipeToDismiss(
        state = dismissState,
        directions = setOf(DismissDirection.EndToStart),
        background = {
            val color = MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp) // match Card padding
                    .clip(RoundedCornerShape(16.dp)) // match Card shape
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(end = 16.dp) // Pull X icon a bit inward
                )
            }
        },
        dismissContent = {
            val isLink = summaryItem.type.equals("link", ignoreCase = true)
            val isScreenshot = summaryItem.type.equals("screenshot", ignoreCase = true)
            val accentColor = when {
                isLink -> AppTheme.colors.accentLink
                isScreenshot -> AppTheme.colors.accentScreenshot
                else -> AppTheme.colors.accentInfo
            }

            Card(
                modifier = modifier
                    .padding(8.dp)
                    .border(
                        width = 1.dp,
                        color = AppTheme.colors.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Left accent strip
                            drawRect(
                                color = accentColor,
                                topLeft = Offset.Zero,
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                        .clickable { expanded = !expanded }
                        .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                ) {
                        // Timestamp pill + type badge row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Timestamp pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.timestampBg
                            ) {
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTheme.colors.timestampText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            // Type badge
                            val badgeLabel = when {
                                isLink -> "Link"
                                isScreenshot -> "Screenshot"
                                else -> summaryItem.type.replaceFirstChar { it.uppercase() }
                            }
                            val badgeBg = when {
                                isLink -> AppTheme.colors.badgeLinkBg
                                isScreenshot -> AppTheme.colors.badgeScreenshotBg
                                else -> AppTheme.colors.badgeInfoBg
                            }
                            val badgeText = when {
                                isLink -> AppTheme.colors.badgeLinkText
                                isScreenshot -> AppTheme.colors.badgeScreenshotText
                                else -> AppTheme.colors.badgeInfoText
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeBg
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // User tags row
                        val userTags = summaryItem.tagList()
                        if (userTags.isNotEmpty() || expanded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                userTags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AppTheme.colors.tagBg
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppTheme.colors.tagText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (expanded) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = AppTheme.colors.filterChipBg,
                                        onClick = { showTagEditor = true }
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Add tag",
                                            tint = AppTheme.colors.filterChipText,
                                            modifier = Modifier.padding(4.dp).size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = summaryItem.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ShareIconButton(summaryItem = summaryItem)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                    Text(
                        text = summaryItem.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                        maxLines = if (expanded) 6 else 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    if (expanded && !summaryItem.link.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = summaryItem.link,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(summaryItem.link))
                                    context.startActivity(intent)
                                }
                        )
                    }

                    if (expanded && summaryItem.type.equals("screenshot", ignoreCase = true) && !summaryItem.imagePath.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val path = summaryItem.imagePath
                                val file = if (path != null) File(path) else null
                                if (file != null && file.exists()) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        context.packageName + ".fileprovider",
                                        file
                                    )
                                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "image/*")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(viewIntent)
                                } else {
                                    Toast.makeText(context, "Screenshot file not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("View screenshot")
                        }
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { context.openSummary(summaryItem.id) }) {
                                Text("Read More")
                            }
                        }
                    }


                    // Expanded-only actions (currently none)
                }
            }
        }
    )

    // Tag editor dialog
    if (showTagEditor) {
        TagEditorDialog(
            currentTags = summaryItem.tagList(),
            previouslyUsedTags = allTags,
            onSave = { newTags ->
                viewModel.updateTags(summaryItem.id, newTags)
                showTagEditor = false
            },
            onDismiss = { showTagEditor = false }
        )
    }
}
@SuppressLint("RememberReturnType")
@Composable
fun rememberFormattedTimestamp(timestamp: Long): String {
    val dateFormat = remember {
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    }
    return dateFormat.format(Date(timestamp))
}




// Reusable share bar for a summary (expanded view)
@Composable
fun ShareBar(summaryItem: SummaryItem) {
    val context = LocalContext.current
    TextButton(
        onClick = { shareSummary(context, summaryItem) }
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share"
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Share")
    }
}

// Compact share icon for header row (non-expanded view)
@Composable
fun ShareIconButton(summaryItem: SummaryItem) {
    val context = LocalContext.current
    Icon(
        imageVector = Icons.Default.Share,
        contentDescription = "Share",
        modifier = Modifier
            .size(20.dp)
            .clickable { shareSummary(context, summaryItem) }
    )
}

private fun shareSummary(context: android.content.Context, summaryItem: SummaryItem) {
    val intent = buildShareSummaryIntent(
        context = context,
        title = summaryItem.title,
        summaryText = summaryItem.summary,
        link = summaryItem.link,
        imagePath = summaryItem.imagePath // will attach screenshot if present
    )

    context.startActivity(Intent.createChooser(intent, "Share summary"))
}

// Preview function with a mock SummaryItem
@Preview(showBackground = true)
@Composable
fun SummaryUiItemPreview() {
    AiSummarizeTheme {
        SummaryUiItem(summaryItem = SummaryItem(
            id = 1,
            summary = "some summary",
            type = "Screenshot",
            link = null,
            title = "someTitle",
            imagePath = null,
            timestamp = System.currentTimeMillis()
        ))
    }
}
