package com.example.aisummarize

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.ui.theme.AiSummarizeTheme

@Composable
fun SummaryUiItem(summaryItem: SummaryItem, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(8.dp)) {
        Column(modifier=Modifier.padding(16.dp)){
            Text(
                text = summaryItem.type,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = summaryItem.summary,
                style =MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(4.dp)
            )
        }

    }
}

// Preview function with a mock SummaryItem
@Preview(showBackground = true)
@Composable
fun SummaryUiItemPreview() {
    AiSummarizeTheme {
        SummaryUiItem(summaryItem = SummaryItem(
            id = 1, summary = "some summary",
            type = "Screenshot",
            content = "content captured",
            timestamp = System.currentTimeMillis()
        ))
    }
}
