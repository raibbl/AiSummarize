package com.example.aisummarize

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.ui.theme.AiSummarizeTheme
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.*

@Composable
fun SummaryUiItem(summaryItem: SummaryItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formattedTime = rememberFormattedTimestamp(summaryItem.timestamp)
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = modifier.padding(8.dp)) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }  // Toggle on card click
            .padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = summaryItem.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Text(
                text = summaryItem.summary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp),
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (expanded) {
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
        }
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


// Preview function with a mock SummaryItem
@Preview(showBackground = true)
@Composable
fun SummaryUiItemPreview() {
    AiSummarizeTheme {
        SummaryUiItem(summaryItem = SummaryItem(
            id = 1, summary = "some summary",
            type = "Screenshot",
            link = "content captured",
            title = "someTitle",
            timestamp = System.currentTimeMillis()
        ))
    }
}
