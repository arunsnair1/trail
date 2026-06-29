package com.caladapt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class required by Hilt for dependency injection setup.
 */
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.caladapt.worker.EveningCalorieReminderWorker
import com.caladapt.worker.MorningWeightReminderWorker
import com.caladapt.worker.WeeklyReviewReadyWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class CalAdaptApplication : Application(), Configuration.Provider {
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupWorkManager()
    }

    private fun setupWorkManager() {
        val workManager = WorkManager.getInstance(this)

        // Morning weight reminder (approx every 24h)
        val morningRequest = PeriodicWorkRequestBuilder<MorningWeightReminderWorker>(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "MorningWeightReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            morningRequest
        )

        // Evening calorie reminder (approx every 24h)
        val eveningRequest = PeriodicWorkRequestBuilder<EveningCalorieReminderWorker>(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "EveningCalorieReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            eveningRequest
        )

        // Weekly review ready (approx every 7 days)
        val weeklyRequest = PeriodicWorkRequestBuilder<WeeklyReviewReadyWorker>(7, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "WeeklyReviewReady",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyRequest
        )
    }
}
