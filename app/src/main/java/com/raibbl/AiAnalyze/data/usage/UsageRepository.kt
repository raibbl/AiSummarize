package com.raibbl.AiAnalyze.data.usage

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UsageData(
    val summaryCount: Int = 0,
    val periodStart: Long = System.currentTimeMillis()
)

class UsageRepository {

    companion object {
        private const val COLLECTION_USAGE = "usage"
        private const val FIELD_SUMMARY_COUNT = "summaryCount"
        private const val FIELD_PERIOD_START = "periodStart"
        private const val PERIOD_MS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun ensureAuthenticated() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private fun getUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User not authenticated. Call ensureAuthenticated() first.")
    }

    suspend fun getOrCreateUsage(): UsageData {
        val uid = getUid()
        val docRef = firestore.collection(COLLECTION_USAGE).document(uid)
        val snapshot = docRef.get().await()

        if (!snapshot.exists()) {
            val newUsage = UsageData()
            docRef.set(
                mapOf(
                    FIELD_SUMMARY_COUNT to newUsage.summaryCount,
                    FIELD_PERIOD_START to newUsage.periodStart
                )
            ).await()
            return newUsage
        }

        val summaryCount = snapshot.getLong(FIELD_SUMMARY_COUNT)?.toInt() ?: 0
        val periodStart = snapshot.getLong(FIELD_PERIOD_START) ?: System.currentTimeMillis()

        // Reset if 30 days have passed
        if (System.currentTimeMillis() - periodStart >= PERIOD_MS) {
            val resetUsage = UsageData()
            docRef.set(
                mapOf(
                    FIELD_SUMMARY_COUNT to resetUsage.summaryCount,
                    FIELD_PERIOD_START to resetUsage.periodStart
                )
            ).await()
            return resetUsage
        }

        return UsageData(summaryCount = summaryCount, periodStart = periodStart)
    }

    suspend fun canSummarize(isPro: Boolean, freeLimit: Int): Boolean {
        if (isPro) return true
        val usage = getOrCreateUsage()
        return usage.summaryCount < freeLimit
    }

    suspend fun incrementCount() {
        val uid = getUid()
        val docRef = firestore.collection(COLLECTION_USAGE).document(uid)
        val usage = getOrCreateUsage()
        docRef.update(FIELD_SUMMARY_COUNT, usage.summaryCount + 1).await()
    }

    suspend fun getRemainingFree(freeLimit: Int): Int {
        val usage = getOrCreateUsage()
        return (freeLimit - usage.summaryCount).coerceAtLeast(0)
    }

    suspend fun getResetDate(): Long {
        val usage = getOrCreateUsage()
        return usage.periodStart + PERIOD_MS
    }
}
