package com.raibbl.AiAnalyze

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.raibbl.AiAnalyze.data.billing.SubscriptionState
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme
import com.raibbl.AiAnalyze.utils.goHomeClearTask
import com.raibbl.AiAnalyze.utils.showToastAndGoHome
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
                showToastAndGoHome("No Valid Link.")

            }
        }
    }

    private fun generateSummaryForLink(link: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check usage limit before summarizing
                val app = application.brieflyApp
                val isPro = app.billingManager.subscriptionState.value is SubscriptionState.Pro
                val freeLimit = app.remoteConfigManager.getFreeSummaryLimit()

                app.usageRepository.ensureAuthenticated()
                if (!app.usageRepository.canSummarize(isPro, freeLimit)) {
                    val resetDate = app.usageRepository.getResetDate()
                    runOnUiThread {
                        val intent = Intent(this@LinkSummaryActivity, UpgradeActivity::class.java).apply {
                            putExtra(UpgradeActivity.EXTRA_FREE_LIMIT, freeLimit)
                            putExtra(UpgradeActivity.EXTRA_RESET_DATE, resetDate)
                        }
                        startActivity(intent)
                        finish()
                    }
                    return@launch
                }

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

                // Increment usage count after successful summary
                app.usageRepository.incrementCount()

                runOnUiThread {
                    saveSummaryAndLaunchUI(result.summary, link, result.title, result.tags)
                }
            } catch (e: Exception) {
                Log.e("failed to summarize using AI", "", e)
                runOnUiThread {
                    showToastAndGoHome("There was an issue getting your summary, please try again later.")


                }

            }

        }
    }
    private fun saveSummaryAndLaunchUI(summary: String, link: String, title: String, tags: List<String> = emptyList()) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@LinkSummaryActivity)
            val summaryDao = db.summaryDao()
            val summaryItem = SummaryItem(
                type = "link",
                link = link,
                title = title,
                summary = summary,
                tags = tags.joinToString(",").ifEmpty { null }
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
