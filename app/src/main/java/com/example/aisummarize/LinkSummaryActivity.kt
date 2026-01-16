package com.example.aisummarize

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.example.aisummarize.data.db.AppDatabase

import com.example.aisummarize.data.db.SummaryItem
import com.example.aisummarize.ui.theme.AiSummarizeTheme
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
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
        val ai = Firebase.ai(backend = GenerativeBackend.googleAI())
        val generativeModel = ai.generativeModel("gemini-2.5-flash-lite")

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

                val fallbackToUrlPrompt = articleText.length < 200

                val summaryResponse = if (fallbackToUrlPrompt) {
                    generativeModel.generateContent(
                        """
                    Read and analyze the full content at the following URL: $link. 
                    Write a concise summary in 1–2 short paragraphs highlighting the main ideas.
                    """.trimIndent()
                    )
                } else {
                    generativeModel.generateContent(
                        """
                    Summarize the following article in 1–2 paragraphs, clearly stating the main ideas:
                    
                    $articleText
                    """.trimIndent()
                    )
                }

                val summaryTitle = generativeModel.generateContent(
                    if (fallbackToUrlPrompt)
                        """
        You are an assistant that returns only one result with no explanation.
        Return exactly one concise, 3 to 5 word title summarizing the content at this URL: $link
        
        Do not include explanations, quotes, or multiple options. Only return the title text.
        """.trimIndent()
                    else
                        """
        You are an assistant that returns only one result with no explanation.
        Return exactly one concise, 3 to 5 word title summarizing the following article:

        $articleText

        Do not include explanations, quotes, or multiple options. Only return the title text.
        """.trimIndent()
                )

                runOnUiThread {
                    if (!summaryResponse.text.isNullOrBlank()) {
                        saveSummaryAndLaunchUI(summaryResponse.text!!, link, summaryTitle.text ?: "Untitled")
                    } else {
                        Toast.makeText(this@LinkSummaryActivity, "Failed to generate summary", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("failed to summarize using AI","",e)
                runOnUiThread {
                    Toast.makeText(this@LinkSummaryActivity, "There was an issue getting your summary, please try again later.", Toast.LENGTH_SHORT).show()
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
