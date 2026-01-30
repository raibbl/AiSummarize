package com.raibbl.AiAnalyze.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.raibbl.AiAnalyze.SummaryGeminiService
import kotlinx.coroutines.launch

enum class SummaryStyle(val label: String, val prompt: String) {
    KEY_POINTS(
        "Key Points",
        "Extract and list the key points as bullet points. Keep it concise with 4-6 main points."
    ),
    TEXT_MESSAGE(
        "Text Message",
        "Summarize this in 2-3 very short sentences that could be sent as a text message. Be extremely brief and casual."
    ),
    SIMPLE(
        "Simple",
        "Explain this in very simple terms as if talking to someone unfamiliar with the topic. Use simple words and short sentences."
    ),
    DETAILED(
        "Detailed",
        "Provide a comprehensive and detailed summary covering all important aspects, context, and implications."
    )
}

@Composable
fun AISummaryAdjustBar(
    originalText: String,
    link: String?,
    onSummaryRegenerated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRegenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "AI Summary Style",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Regenerate summary with a different style",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))

            if (isRegenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.padding(8.dp))
                    Text("Regenerating summary...")
                }
            } else {
                // Summary style options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryStyle.entries.forEach { style ->
                        ElevatedButton(
                            onClick = {
                                isRegenerating = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val newSummary = regenerateSummary(
                                            originalText = originalText,
                                            style = style,
                                            link = link
                                        )
                                        onSummaryRegenerated(newSummary)
                                        isRegenerating = false
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to regenerate: ${e.message}"
                                        isRegenerating = false
                                    }
                                }
                            }
                        ) {
                            Text(style.label)
                        }
                    }
                }
            }
        }
    }
}

private suspend fun regenerateSummary(
    originalText: String,
    style: SummaryStyle,
    link: String?
): String {
    val prompt = buildString {
        append(style.prompt)
        append("\n\nContent to summarize:\n")
        append(originalText)
        if (!link.isNullOrBlank()) {
            append("\n\nSource: $link")
        }
    }
    
    return SummaryGeminiService.regenerateWithStyle(prompt)
}
