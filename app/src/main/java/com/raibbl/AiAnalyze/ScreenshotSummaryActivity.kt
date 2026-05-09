package com.raibbl.AiAnalyze

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import kotlinx.coroutines.launch

class ScreenshotSummaryActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSummaryText = intent.getStringExtra("EXTRA_SUMMARY")
        val imagePath = intent.getStringExtra("EXTRA_SCREENSHOT_PATH")

        if (initialSummaryText.isNullOrBlank()) {
            Toast.makeText(this, "No summary to display", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AiSummarizeTheme {
                var currentSummary by remember { mutableStateOf(initialSummaryText) }

                SummaryContent(
                    summaryId = -1, // screenshots opened via extras (no stable id provided)
                    title = "Screenshot summary",
                    summaryText = currentSummary,
                    link = null,
                    imagePath = imagePath,
                    onSummaryChanged = { newSummary ->
                        // Update UI immediately
                        currentSummary = newSummary

                        // Best-effort persist (requires DAO method below)
                        if (!imagePath.isNullOrBlank()) {
                            lifecycleScope.launch {
                                val db = AppDatabase.getDatabase(this@ScreenshotSummaryActivity)
                                val dao = db.summaryDao()

                                // Requires: suspend fun getSummaryByImagePath(path: String): SummaryItem?
                                val item = dao.getSummaryByImagePath(imagePath)

                                if (item != null) {
                                    dao.updateSummary(item.copy(summary = newSummary))
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
