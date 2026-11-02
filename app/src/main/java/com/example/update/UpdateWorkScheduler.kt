package com.example.update

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object UpdateWorkScheduler {

    private const val TAG = "UpdateWorkScheduler"
    private const val WORK_NAME_PERIODIC = "periodic_app_update_check"
    private const val WORK_NAME_IMMEDIATE = "immediate_app_update_check"

    /**
     * Schedules periodic background update checks using WorkManager.
     * Enforces NetworkType.CONNECTED constraint so it only runs when device has internet access.
     * WorkManager automatically survives device reboots and app closures.
     */
    fun schedulePeriodicCheck(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Run check every 6 hours with internet requirement
            val periodicWorkRequest = PeriodicWorkRequestBuilder<AppUpdateCheckWorker>(
                6L, TimeUnit.HOURS,
                30L, TimeUnit.MINUTES // 30-min flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            Log.d(TAG, "Periodic update check scheduled via WorkManager (Interval: 6h, Network: CONNECTED)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic update check: ${e.message}", e)
        }
    }

    /**
     * Enqueues an immediate one-time background check when app launches or device boots.
     */
    fun enqueueImmediateCheck(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<AppUpdateCheckWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest
            )
            Log.d(TAG, "Immediate update check enqueued via WorkManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue immediate update check: ${e.message}", e)
        }
    }
}
