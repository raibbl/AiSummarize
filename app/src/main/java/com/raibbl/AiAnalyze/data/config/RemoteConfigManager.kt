package com.raibbl.AiAnalyze.data.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteConfigManager {

    companion object {
        private const val KEY_FREE_SUMMARY_LIMIT = "free_summary_limit"
        private const val DEFAULT_FREE_LIMIT = 40L
    }

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(KEY_FREE_SUMMARY_LIMIT to DEFAULT_FREE_LIMIT)
        )
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false
        }
    }

    fun getFreeSummaryLimit(): Int {
        return remoteConfig.getLong(KEY_FREE_SUMMARY_LIMIT).toInt()
    }
}
