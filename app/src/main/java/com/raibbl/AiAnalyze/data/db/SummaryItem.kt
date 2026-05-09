package com.raibbl.AiAnalyze.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summaries")
data class SummaryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "link" or "screenshot"
    val link: String? = null,
    val summary: String,
    val title: String,
    val imagePath: String? = null,
    val tags: String? = null, // comma-separated, e.g. "Tech,AI"
    val timestamp: Long = System.currentTimeMillis()
) {
    /** Parsed tag list (empty if null/blank). */
    fun tagList(): List<String> =
        tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}
