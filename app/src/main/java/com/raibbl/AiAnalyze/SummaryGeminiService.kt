package com.raibbl.AiAnalyze

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend

object SummaryGeminiService {

    data class LinkSummaryResult(
        val summary: String,
        val title: String,
        val tags: List<String> = emptyList()
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

        // Generate title + tags in a single call for efficiency
        val titleTagPrompt = """
            Based on this summary, return exactly two lines in this format:
            Title: <3-5 word title>
            Tags: <1-3 category tags, comma-separated>

            Summary: $summaryText

            Use general, professional categories for tags. Do not include any other text or explanations.
        """.trimIndent()

        val titleTagResponse = generativeModel.generateContent(titleTagPrompt)
        val responseText = titleTagResponse.text?.trim() ?: ""
        
        // Parse title - look for "Title:" prefix or use first line as fallback
        val titleText = when {
            responseText.contains("Title:", ignoreCase = true) -> {
                responseText.lines()
                    .firstOrNull { it.contains("Title:", ignoreCase = true) }
                    ?.substringAfter(":", "")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Untitled"
            }
            else -> responseText.lines().firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: "Untitled"
        }
        
        // Parse tags - look for "Tags:" prefix or use second line as fallback
        val tags = when {
            responseText.contains("Tags:", ignoreCase = true) -> {
                responseText.lines()
                    .firstOrNull { it.contains("Tags:", ignoreCase = true) }
                    ?.substringAfter(":", "")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }
            else -> {
                responseText.lines().getOrNull(1)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?: emptyList()
            }
        }

        return LinkSummaryResult(summary = summaryText, title = titleText, tags = tags)
    }

    /**
     * Regenerate a summary with a specific style prompt.
     */
    suspend fun regenerateWithStyle(prompt: String): String {
        val response = generativeModel.generateContent(prompt)
        return response.text?.trim()
            ?: throw IllegalStateException("Empty summary returned from Gemini")
    }
}
