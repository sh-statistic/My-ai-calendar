package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms all future event reminder alarms after the device boots, the app
 * is updated, or the clock/timezone changes. Android drops every AlarmManager
 * registration on reboot, so without this receiver all event alarms —
 * including every occurrence of a recurring series — would stay silent
 * until the event is created or edited again (spec
 * 001-recurring-event-reminder, FR-007).
 *
 * Every stored row is offered to [EventAlarmScheduler.rearmAll], which
 * cancels any stale registration first and then arms the still-valid ones —
 * so this also sweeps away ghost alarms left on soft-deleted rows.
 */
class EventAlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val events = AppDatabase.getInstance(context).eventDao().getAllEventsForSync()
                EventAlarmScheduler.rearmAll(context, events)
                Log.d(TAG, "Re-armed event alarms for ${events.count { !it.isDeleted }} active events after $action")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm event alarms: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "EventAlarmBootReceiver"
    }
}
