package com.example.aisummarize

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
                val prompt = "Summarize the content of the following URL: $link"
                val response = generativeModel.generateContent(prompt)

                // Handle the summary response on the main thread
                runOnUiThread {
                    if (response.text != null) {
                        startSummaryActivity(response.text!!)
                    } else {
                        Toast.makeText(this@LinkSummaryActivity, "Failed to generate summary", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@LinkSummaryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startSummaryActivity(summary: String) {
        // Start SummaryActivity with the generated summary
        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra("EXTRA_SUMMARY", summary)
        }
        startActivity(intent)
        finish() // Close the current activity
    }
}
