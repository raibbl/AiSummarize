package com.example.aisummarize

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.aisummarize.data.db.AppDatabase

import com.example.aisummarize.data.db.SummaryItem
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LinkSummaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the shared intent
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
        // Initialize the Vertex AI model
        val vertexAI = Firebase.vertexAI
        val generativeModel = vertexAI.generativeModel("gemini-1.5-flash")

        // Use a coroutine to call Vertex AI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val promptForSummary = """
    Read and analyze the full content at the following URL: $link. 
    Write a clear and concise summary in 1–2 short paragraphs that highlight the main points, key takeaways, and any important conclusions from the article. 
    Keep the tone neutral and informative, avoid filler or opinions, and ensure the summary is easy to read at a glance.
""".trimIndent()
                val promptForSummaryTitle =
                    "Return exactly one short, 3 to 5 word title summarizing the page at this URL: $link. Do not include any explanations or multiple options. Only return the title as plain text."
                val summaryResponse = generativeModel.generateContent(promptForSummary)
                val summaryTitle = generativeModel.generateContent(promptForSummaryTitle)

                // Handle the summary response on the main thread
                runOnUiThread {
                    if (summaryResponse.text != null) {
                        saveSummaryAndLaunchUI(summaryResponse.text!!, link, summaryTitle.text!!)
                    } else {
                        Toast.makeText(
                            this@LinkSummaryActivity,
                            "Failed to generate summary",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@LinkSummaryActivity,
                        "Error: ${e.message}",
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
