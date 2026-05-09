package com.raibbl.AiAnalyze.data.db

import androidx.lifecycle.LiveData
import androidx.room.*


@Dao
interface SummaryDao {

    // Insert a new summary item into the database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summaryItem: SummaryItem): Long

    // Update an existing summary item
    @Update
    suspend fun updateSummary(summaryItem: SummaryItem)

    // Fetch all summaries from the database, ordered by the newest first
    @Query("SELECT * FROM summaries ORDER BY timestamp DESC")
    fun getAllSummaries(): LiveData<List<SummaryItem>>

    @Query("SELECT * FROM summaries WHERE summary LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR link LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchSummaries(query: String): LiveData<List<SummaryItem>>


    // Fetch a single summary by ID
    @Query("SELECT * FROM summaries WHERE id = :id")
    suspend fun getSummaryById(id: Int): SummaryItem?

    // Delete a single summary by ID
    @Query("DELETE FROM summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Int)

    // Delete all summaries
    @Query("DELETE FROM summaries")
    suspend fun deleteAllSummaries()

    // Count summaries (used to seed an onboarding item once)
    @Query("SELECT COUNT(*) FROM summaries")
    suspend fun getSummaryCount(): Int

    @Query("SELECT * FROM summaries WHERE imagePath = :path LIMIT 1")
    suspend fun getSummaryByImagePath(path: String): SummaryItem?

    // Update tags for a specific summary
    @Query("UPDATE summaries SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Int, tags: String?)

    // Get all distinct tags (returns raw comma-separated strings; parse in ViewModel)
    @Query("SELECT DISTINCT tags FROM summaries WHERE tags IS NOT NULL AND tags != ''")
    fun getAllRawTags(): LiveData<List<String>>
}
