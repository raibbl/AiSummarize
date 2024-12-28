package com.example.aisummarize.data.db

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface SummaryDao {

    // Insert a new summary item into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summaryItem: SummaryItem)

    // Fetch all summaries from the database, ordered by the newest first
    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): LiveData<List<SummaryItem>>

    // Fetch a single summary by ID
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: Int): SummaryItem?

    // Delete a single summary by ID
    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Int)

    // Delete all summaries
    @Query("DELETE FROM summaries")
    suspend fun deleteAllSummaries()
}
