package com.raibbl.AiAnalyze.ui.components

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import com.raibbl.AiAnalyze.ui.theme.AppTheme
import com.raibbl.AiAnalyze.models.SummaryViewModel
import com.raibbl.AiAnalyze.ui.components.BUILT_IN_SUGGESTIONS
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raibbl.AiAnalyze.models.SummaryViewModelFactory

@Composable
fun SummaryList(summaryItems: List<SummaryItem>, modifier: Modifier = Modifier) {
    if (summaryItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppTheme.colors.emptyIcon
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No summaries yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.emptyText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Share a link or capture your screen\nto create your first summary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.emptyText,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(modifier = modifier.padding(8.dp)) {
            items(summaryItems, key = { it.id }) { summaryItem ->
                SummaryUiItem(
                    summaryItem = summaryItem,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SummaryListFromDb(modifier: Modifier=Modifier) {
    val application = LocalContext.current.applicationContext as Application
    val factory = SummaryViewModelFactory(application)
    val viewModel: SummaryViewModel = viewModel(factory = factory)
    var searchText by remember { mutableStateOf("") }
    val summaryItems by viewModel.summaries.observeAsState(emptyList())
    val allTags by viewModel.allTags.observeAsState(emptyList())
    val activeFilters by viewModel.activeFilters.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.insertOnboardingIfEmpty()
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.padding(8.dp)) {
        // 🔍 Search Bar + Filter icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.updateSearchQuery(it)
                },
                label = { Text("Search summaries...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
            IconButton(onClick = { showFilterDialog = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter by tags",
                    tint = if (activeFilters.isNotEmpty()) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Active filter chips (dismissible)
        if (activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                activeFilters.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AppTheme.colors.filterChipSelectedBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.filterChipSelectedText
                            )
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tag filter",
                                tint = AppTheme.colors.filterChipSelectedText,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(start = 4.dp)
                                    .clickable { viewModel.toggleFilter(tag) }
                            )
                        }
                    }
                }
            }
        }

        // 📝 Filtered list
        SummaryList(summaryItems = summaryItems)
    }

    // Filter dialog
    if (showFilterDialog) {
        TagFilterDialog(
            allTags = (allTags + BUILT_IN_SUGGESTIONS).distinct(),
            activeFilters = activeFilters,
            onToggle = { viewModel.toggleFilter(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagFilterDialog(
    allTags: List<String>,
    activeFilters: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
        title = { Text("Filter by Tags") },
        text = {
            if (allTags.isEmpty()) {
                Text("No tags available yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allTags.forEach { tag ->
                        val selected = tag in activeFilters
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) AppTheme.colors.filterChipSelectedBg else AppTheme.colors.filterChipBg,
                            onClick = { onToggle(tag) }
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) AppTheme.colors.filterChipSelectedText else AppTheme.colors.filterChipText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SummaryListPreview(modifier: Modifier=Modifier) {
    val sampleItems = listOf(
        SummaryItem(
            type = "Screenshot", summary = "Breaking news: Compose simplifies UI development.",
            id = 2,
            link = "someContent",
            title = "SomeTitle",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Link",
            summary = "Learn how Kotlin Coroutines improve Android development.",
            id = 3,
            link = "someContent",
            title = "SomeTitle",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Screenshot", summary = "10 tips for a healthier lifestyle.", id = 4,
            link = "someContent",
            title = "SomeTitle",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Link", summary = "NASA announces new Mars mission.", id = 5,
            link = "someContent",
            title = "SomeTitle",
            timestamp = System.currentTimeMillis()
        ),
    )

    AiSummarizeTheme {
        SummaryList(summaryItems = sampleItems, modifier = modifier)
    }
}