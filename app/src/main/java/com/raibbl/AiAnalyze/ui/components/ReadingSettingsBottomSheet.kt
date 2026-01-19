package com.raibbl.AiAnalyze.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Reading settings state model.
 * Keep this in the same file as the bottom sheet so it is portable and easy to reuse.
 */
data class ReadingSettings(
    val sizePreset: TextSizePreset = TextSizePreset.DEFAULT,
    val comfortableSpacing: Boolean = true,
    val centerText: Boolean = false
)




val ReadingSettingsSaver: Saver<ReadingSettings, Any> = listSaver(
    save = { settings ->
        listOf(
            settings.sizePreset.name,          // String
            settings.comfortableSpacing,       // Boolean
            settings.centerText                // Boolean
        )
    },
    restore = { list ->
        ReadingSettings(
            sizePreset = TextSizePreset.valueOf(list[0] as String),
            comfortableSpacing = list[1] as Boolean,
            centerText = list[2] as Boolean
        )
    }
)


/**
 * Discrete size presets. Use scale factors so the app still respects Material typography choices.
 */
enum class TextSizePreset(val scale: Float, val label: String) {
    SMALL(0.90f, "Small"),
    DEFAULT(1.00f, "Default"),
    LARGE(1.15f, "Large"),
    XL(1.30f, "XL")
}

/**
 * UI constants for the Reading Settings bottom sheet.
 */
object ReadingSheetDefaults {
    val ContentPadding = 16.dp
    val SectionSpacing = 12.dp
    val ChipRowSpacing = 8.dp
    val ChipSpacing = 8.dp
}

/**
 * Bottom sheet for adjusting reading / readability preferences.
 *
 * Usage:
 *   ReadingSettingsBottomSheet(
 *     settings = readingSettings,
 *     onSettingsChange = { readingSettings = it },
 *     onDismiss = { showReading = false }
 *   )
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsBottomSheet(
    settings: ReadingSettings,
    onSettingsChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = modifier.padding(ReadingSheetDefaults.ContentPadding)
        ) {

            Spacer(Modifier.height(ReadingSheetDefaults.SectionSpacing))

            Text(
                text = "Text size",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.height(ReadingSheetDefaults.ChipRowSpacing))

            Row(horizontalArrangement = Arrangement.spacedBy(ReadingSheetDefaults.ChipSpacing)) {
                TextSizePreset.entries.forEach { preset ->
                    val selected = settings.sizePreset == preset
                    FilterChip(
                        selected = selected,
                        onClick = { onSettingsChange(settings.copy(sizePreset = preset)) },
                        label = { Text(preset.label) }
                    )
                }
            }

            Spacer(Modifier.height(ReadingSheetDefaults.SectionSpacing))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Comfortable line spacing",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.comfortableSpacing,
                    onCheckedChange = { onSettingsChange(settings.copy(comfortableSpacing = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Center text",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = settings.centerText,
                    onCheckedChange = { onSettingsChange(settings.copy(centerText = it)) }
                )
            }

            Spacer(Modifier.height(ReadingSheetDefaults.SectionSpacing))
        }
    }
}

/**
 * Preview:
 * Note: ModalBottomSheet renders as if it is shown; in Preview it will appear "open".
 * The dismiss handler is a no-op for preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun ReadingSettingsBottomSheetPreview() {
    val (settings, setSettings) = remember { mutableStateOf(ReadingSettings()) }

    MaterialTheme {
        ReadingSettingsBottomSheet(
            settings = settings,
            onSettingsChange = setSettings,
            onDismiss = { /* no-op for preview */ }
        )
    }
}
