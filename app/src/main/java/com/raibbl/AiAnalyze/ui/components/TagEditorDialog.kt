package com.raibbl.AiAnalyze.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.raibbl.AiAnalyze.ui.theme.AppTheme

val BUILT_IN_SUGGESTIONS = listOf(
    "Tech", "News", "Health", "Finance", "Learning", "Work", "Personal", "Science", "Sports"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagEditorDialog(
    currentTags: List<String>,
    previouslyUsedTags: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var tags by remember { mutableStateOf(currentTags.toMutableList()) }
    var inputText by remember { mutableStateOf("") }

    // Suggestions = previously used + built-in, minus already-applied tags
    val suggestions = remember(tags, previouslyUsedTags) {
        (previouslyUsedTags + BUILT_IN_SUGGESTIONS)
            .distinct()
            .filter { it !in tags }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(tags) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Edit Tags") },
        text = {
            Column {
                // Current tags
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.tagBg
                            ) {
                                TextButton(
                                    onClick = { tags = tags.toMutableList().also { it.remove(tag) } }
                                ) {
                                    Text(tag, color = AppTheme.colors.tagText)
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $tag",
                                        tint = AppTheme.colors.tagText,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Add a tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(onClick = {
                                val newTag = inputText.trim()
                                if (newTag.isNotEmpty() && newTag !in tags) {
                                    tags = tags.toMutableList().also { it.add(newTag) }
                                }
                                inputText = ""
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add tag")
                            }
                        }
                    }
                )

                // Suggestions
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AppTheme.colors.filterChipBg,
                                onClick = {
                                    if (suggestion !in tags) {
                                        tags = tags.toMutableList().also { it.add(suggestion) }
                                    }
                                }
                            ) {
                                Text(
                                    text = suggestion,
                                    color = AppTheme.colors.filterChipText,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
