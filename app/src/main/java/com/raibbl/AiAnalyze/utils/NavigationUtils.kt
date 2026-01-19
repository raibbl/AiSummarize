package com.raibbl.AiAnalyze.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.raibbl.AiAnalyze.MainActivity
import com.raibbl.AiAnalyze.SummaryActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val EXTRA_SUMMARY_ID = "EXTRA_SUMMARY_ID"

fun Activity.goHomeClearTask() {
    val intent = Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    startActivity(intent)
    finish()
}

fun Context.openSummary(summaryId: Int) {
    val intent = Intent(this, SummaryActivity::class.java).apply {
        putExtra(EXTRA_SUMMARY_ID, summaryId)
    }
    startActivity(intent)
}

fun Activity.showToastAndGoHome(message: String, toastLength: Int = Toast.LENGTH_SHORT) {
    if (this is LifecycleOwner) {
        Toast.makeText(this, message, toastLength).show()

        val delayMillis = if (toastLength == Toast.LENGTH_SHORT) 2000L else 3500L

        lifecycleScope.launch {
            delay(delayMillis)
            goHomeClearTask()
        }
    }
}
