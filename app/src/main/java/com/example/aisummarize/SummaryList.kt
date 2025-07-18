package com.example.aisummarize

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.ui.theme.AiSummarizeTheme
import models.SummaryViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import models.SummaryViewModelFactory

@Composable
fun SummaryList(summaryItems: List<SummaryItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(summaryItems) { summaryItem ->
            SummaryUiItem(summaryItem = summaryItem, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SummaryListFromDb(modifier: Modifier=Modifier) {
    val application = LocalContext.current.applicationContext as Application
    val factory = SummaryViewModelFactory(application)
    val viewModel: SummaryViewModel = viewModel(factory = factory)

    val summaryItems by viewModel.summaries.observeAsState(emptyList())

    AiSummarizeTheme {
        SummaryList(summaryItems = summaryItems, modifier = modifier)
    }

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