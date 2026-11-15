package com.example.announce

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Announcement checks: a periodic background check every 6 hours (the same
 * cadence as the update checker) plus an immediate one on app start. Both
 * need a connected network; WorkManager keeps them across reboots.
 */
object AnnouncementScheduler {

    private const val WORK_NAME_PERIODIC = "periodic_announcement_check"
    private const val WORK_NAME_IMMEDIATE = "immediate_announcement_check"

    private fun connectedConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

    fun schedulePeriodicCheck(context: Context) {
        try {
            val request = PeriodicWorkRequestBuilder<AnnouncementCheckWorker>(
                6L, TimeUnit.HOURS,
                30L, TimeUnit.MINUTES
            )
                .setConstraints(connectedConstraints())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        } catch (e: Exception) {
            // Checks are best-effort; the next app start tries again.
        }
    }

    fun enqueueImmediateCheck(context: Context) {
        try {
            val request = OneTimeWorkRequestBuilder<AnnouncementCheckWorker>()
                .setConstraints(connectedConstraints())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            // Same as above: best-effort.
        }
    }
}
