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
            Summarize the key information from this screen capture. Include the main points and any important details or outcomes in 4-5 sentences:

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
            Read the content at this URL: $link
            Write a concise summary that captures the main ideas, key points, and any important conclusions in 5-6 sentences.
            """.trimIndent()
        } else {
            """
            Write a concise summary of this article that captures the main ideas, key points, and any important conclusions in 5-6 sentences:

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
