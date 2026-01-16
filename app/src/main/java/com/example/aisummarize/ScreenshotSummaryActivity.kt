package com.example.aisummarize

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aisummarize.ui.theme.AiSummarizeTheme

class ScreenshotSummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val summaryText = intent.getStringExtra("EXTRA_SUMMARY")
        val imagePath = intent.getStringExtra("EXTRA_SCREENSHOT_PATH")

        if (summaryText.isNullOrBlank()) {
            Toast.makeText(this, "No summary to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AiSummarizeTheme {
                SummaryContent(
                    title = "Screenshot summary",
                    summaryText = summaryText,
                    link = null,
                    imagePath = imagePath
                )
            }
        }
    }
}
