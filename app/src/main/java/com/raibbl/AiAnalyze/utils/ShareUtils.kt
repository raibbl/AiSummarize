package com.raibbl.AiAnalyze.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun buildShareSummaryIntent(
    context: Context,
    title: String,
    summaryText: String,
    link: String? = null,
    imagePath: String? = null
): Intent {
    val text = buildString {
        append(title)
        append("\n\n")
        append(summaryText)
        if (!link.isNullOrBlank()) {
            append("\n\n")
            append("Link: $link")
        }
    }

    val file = imagePath?.let { File(it) }
    val hasImage = file != null && file.exists()

    return if (hasImage) {
        val uri = file?.let {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                it
            )
        }

        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }
}
