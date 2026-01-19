package com.raibbl.AiAnalyze

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.raibbl.AiAnalyze.ui.components.SummaryListFromDb
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.ui.theme.AiSummarizeTheme


class MainActivity : ComponentActivity() {
    companion object {
        lateinit var database: AppDatabase
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize the database
        database = AppDatabase.getDatabase(this)

        enableEdgeToEdge()
        setContent {
            AiSummarizeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SummaryListFromDb(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
