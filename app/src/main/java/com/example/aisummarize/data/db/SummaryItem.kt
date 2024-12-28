package com.example.aisummarize.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class SummaryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "link" or "screenshot"
    val content: String,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)
