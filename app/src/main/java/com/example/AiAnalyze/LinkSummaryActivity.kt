package com.raibbl.AiAnalyze

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkSummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiSummarizeTheme {
                SummarizeLoadingScreen()
            }
        }

        handleShareIntent()
    }


    private fun handleShareIntent() {
        val intent = intent
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null && sharedText.startsWith("http")) {
                generateSummaryForLink(sharedText)
            } else {
                Toast.makeText(this, "No valid link shared!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun generateSummaryForLink(link: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try fetching article text from HTML
                val articleText = try {
                    val document = org.jsoup.Jsoup.connect(link).get()
                    document.select("article").text().ifEmpty {
                        document.body().text()
                    }
                } catch (e: Exception) {
                    "" // CAPTCHA, timeout, etc.
                }

                val result = SummaryGeminiService.summarizeLink(link = link, articleText = articleText)

                runOnUiThread {
                    saveSummaryAndLaunchUI(result.summary, link, result.title)
                }
            } catch (e: Exception) {
                Log.e("failed to summarize using AI", "", e)
                runOnUiThread {
                    Toast.makeText(
                        this@LinkSummaryActivity,
                        "There was an issue getting your summary, please try again later.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun saveSummaryAndLaunchUI(summary: String, link: String, title: String) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@LinkSummaryActivity)
            val summaryDao = db.summaryDao()
            val summaryItem = SummaryItem(
                type = "link",
                link = link,
                title = title,
                summary = summary
            )
            val id = summaryDao.insertSummary(summaryItem).toInt()
            val intent = Intent(this@LinkSummaryActivity, SummaryActivity::class.java).apply {
                putExtra("EXTRA_SUMMARY_ID", id)
            }
            startActivity(intent)
            finish()
        }
    }
}
