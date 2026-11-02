package com.example.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver responsible for receiving BOOT_COMPLETED and MY_PACKAGE_REPLACED events.
 * It immediately re-arms the WorkManager periodic update checks and any pending reminders.
 */
class BackgroundUpdateCheckReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_BACKGROUND_UPDATE = "com.example.ACTION_CHECK_BACKGROUND_UPDATE"
        private const val TAG = "BackgroundUpdateCheck"

        fun schedulePeriodicCheck(context: Context) {
            UpdateWorkScheduler.schedulePeriodicCheck(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BackgroundUpdateCheckReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-arm periodic schedule via WorkManager
            UpdateWorkScheduler.schedulePeriodicCheck(context)
            UpdateWorkScheduler.enqueueImmediateCheck(context)

            // Also re-arm 18:00 reminder if there was a pending update not installed yet
            val pending = AppUpdateManager.getPendingUpdateInfo(context)
            if (pending != null && pending.isAvailable) {
                UpdateReminderReceiver.scheduleReminder(context, pending)
            }
            return
        }

        if (action == ACTION_CHECK_BACKGROUND_UPDATE) {
            UpdateWorkScheduler.schedulePeriodicCheck(context)
            UpdateWorkScheduler.enqueueImmediateCheck(context)
        }
    }
}
