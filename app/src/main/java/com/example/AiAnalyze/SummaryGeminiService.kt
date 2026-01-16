package com.raibbl.AiAnalyze

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

object SummaryGeminiService {

    data class LinkSummaryResult(
        val summary: String,
        val title: String
    )

    private val generativeModel by lazy {
        val ai = Firebase.ai(backend = GenerativeBackend.googleAI())
        ai.generativeModel("gemini-2.5-flash-lite")
    }

    /**
     * Summarize text captured from the screen.
     */
    suspend fun summarizeScreenText(inputText: String): String {
        val prompt = """
            The following text is captured from a screen. Please provide a concise and clear summary of the key points:

            "$inputText"
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        return response.text?.trim()
            ?: throw IllegalStateException("Empty summary returned from Gemini")
    }

    /**
     * Summarize a link, optionally using extracted article text.
     * If the article text is short, the model is asked to read from the URL directly.
     */
    suspend fun summarizeLink(link: String, articleText: String): LinkSummaryResult {
        val fallbackToUrlPrompt = articleText.length < 200

        val summaryPrompt = if (fallbackToUrlPrompt) {
            """
            Read and analyze the full content at the following URL: $link.
            Write a concise summary in 1–2 short paragraphs highlighting the main ideas.
            """.trimIndent()
        } else {
            """
            Summarize the following article in 1–2 paragraphs, clearly stating the main ideas:

            $articleText
            """.trimIndent()
        }

        val summaryResponse = generativeModel.generateContent(summaryPrompt)
        val summaryText = summaryResponse.text?.trim()
            ?: throw IllegalStateException("Empty summary returned from Gemini")

        val titlePrompt = if (fallbackToUrlPrompt) {
            """
            You are an assistant that returns only one result with no explanation.
            Return exactly one concise, 3 to 5 word title summarizing the content at this URL: $link

            Do not include explanations, quotes, or multiple options. Only return the title text.
            """.trimIndent()
        } else {
            """
            You are an assistant that returns only one result with no explanation.
            Return exactly one concise, 3 to 5 word title summarizing the following article:

            $articleText

            Do not include explanations, quotes, or multiple options. Only return the title text.
            """.trimIndent()
        }

        val titleResponse = generativeModel.generateContent(titlePrompt)
        val titleText = titleResponse.text?.trim().takeUnless { it.isNullOrBlank() } ?: "Untitled"

        return LinkSummaryResult(summary = summaryText, title = titleText)
    }
}
