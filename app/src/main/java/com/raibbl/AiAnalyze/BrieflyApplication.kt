package com.raibbl.AiAnalyze

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.raibbl.AiAnalyze.data.billing.BillingManager
import com.raibbl.AiAnalyze.data.config.RemoteConfigManager
import com.raibbl.AiAnalyze.data.usage.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BrieflyApplication : Application() {

    val billingManager: BillingManager by lazy { BillingManager(this) }
    val remoteConfigManager: RemoteConfigManager by lazy { RemoteConfigManager() }
    val usageRepository: UsageRepository by lazy { UsageRepository() }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Manage billing connection based on app foreground/background state
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                billingManager.startConnection()
            }

            override fun onStop(owner: LifecycleOwner) {
                billingManager.endConnection()
            }
        })

        // Fetch remote config
        applicationScope.launch {
            remoteConfigManager.fetchAndActivate()
        }

        // Sign in anonymously so usage tracking is ready for any activity
        applicationScope.launch {
            try {
                usageRepository.ensureAuthenticated()
            } catch (e: Exception) {
                // Non-fatal — will retry when usage is actually needed
            }
        }
    }
}

/** Convenience extension to avoid casting in every activity. */
val Application.brieflyApp: BrieflyApplication
    get() = this as BrieflyApplication
