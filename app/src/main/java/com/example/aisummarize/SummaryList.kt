package com.example.aisummarize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.ui.theme.AiSummarizeTheme

@Composable
fun SummaryList(summaryItems: List<SummaryItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp)) {
        summaryItems.forEach { summaryItem ->
            SummaryUiItem(summaryItem = summaryItem, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SummaryListPreview(modifier: Modifier=Modifier) {
    val sampleItems = listOf(
        SummaryItem(
            type = "Screenshot", summary = "Breaking news: Compose simplifies UI development.",
            id = 2,
            content = "someContent",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Link",
            summary = "Learn how Kotlin Coroutines improve Android development.",
            id = 3,
            content = "someContent",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Screenshot", summary = "10 tips for a healthier lifestyle.", id = 4,
            content = "someContent",
            timestamp = System.currentTimeMillis()
        ),
        SummaryItem(
            type = "Link", summary = "NASA announces new Mars mission.", id = 5,
            content = "someContent",
            timestamp = System.currentTimeMillis()
        ),
    )

    AiSummarizeTheme {
        SummaryList(summaryItems = sampleItems, modifier = modifier)
    }
}