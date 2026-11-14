package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import java.util.Calendar
import java.util.Date

object EventAlarmScheduler {

    /**
     * Pure trigger-time calculation for one event row, extracted so it can be
     * unit-tested. Returns the alarm trigger in epoch millis, or null when the
     * event must not ring (deleted, no start time, reminder set to
     * «بدون زنگ», unparseable date, or the trigger is not in the future).
     */
    fun triggerTimeMillis(event: EventEntity, nowMs: Long = System.currentTimeMillis()): Long? {
        if (event.startTime.isBlank() || event.isDeleted) return null

        // Parse reminder option from description
        val reminderMatch = Regex("\\[یادآوری:([^\\]]+)\\]").find(event.description)
        val reminderStr = reminderMatch?.groupValues?.getOrNull(1)?.trim() ?: "همزمان با رویداد"
        if (reminderStr == "بدون زنگ") return null

        val offsetMinutes = when (reminderStr) {
            "۵ دقیقه قبل" -> 5
            "۱۰ دقیقه قبل" -> 10
            "۱۵ دقیقه قبل" -> 15
            "۳۰ دقیقه قبل" -> 30
            "۱ ساعت قبل" -> 60
            "۱ روز قبل" -> 1440
            else -> 0 // همزمان با رویداد
        }

        // Convert Persian date to Gregorian
        val pParts = event.persianDate.split("/").mapNotNull { it.toIntOrNull() }
        if (pParts.size != 3) return null
        val gDate = PersianCalendarHelper.jalaliToGregorian(pParts[0], pParts[1], pParts[2])

        val timeParts = event.startTime.split(":").mapNotNull { it.toIntOrNull() }
        val hour = if (timeParts.isNotEmpty()) timeParts[0] else 10
        val minute = if (timeParts.size > 1) timeParts[1] else 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gDate.year)
            set(Calendar.MONTH, gDate.month - 1)
            set(Calendar.DAY_OF_MONTH, gDate.day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -offsetMinutes)
        }

        val triggerMs = cal.timeInMillis
        return if (triggerMs <= nowMs) null else triggerMs
    }

    fun scheduleEventAlarm(context: Context, event: EventEntity) {
        try {
            val triggerMs = triggerTimeMillis(event) ?: return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = EventAlarmReceiver.ACTION_EVENT_REMINDER
                putExtra("event_id", event.id)
                putExtra("event_title", event.title)
                putExtra("event_desc", "ساعت ${event.startTime} - ${event.description.replace(Regex("\\[یادآوری:[^\\]]+\\]"), "").trim()}")
            }

            val requestCode = event.id.hashCode()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
            Log.d("EventAlarmScheduler", "Alarm scheduled for event '${event.title}' at ${Date(triggerMs)}")
        } catch (e: Exception) {
            Log.e("EventAlarmScheduler", "Error scheduling alarm: ${e.message}")
        }
    }

    /**
     * Reconciles AlarmManager registrations with [events]: every event's old
     * registration is cancelled first, then non-deleted events are armed
     * again. Used after boot, after clock/timezone changes and after a
     * backup restore. The cancel-first step matters when registrations may
     * still exist (time changes, restores): an event whose trigger has
     * become invalid (past, «بدون زنگ», deleted) must not keep a stale
     * alarm, and a soft-deleted row must not leave a ghost behind.
     */
    fun rearmAll(context: Context, events: List<EventEntity>) {
        events.forEach { event ->
            cancelEventAlarm(context, event.id)
            if (!event.isDeleted) scheduleEventAlarm(context, event)
        }
    }

    fun cancelEventAlarm(context: Context, eventId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = EventAlarmReceiver.ACTION_EVENT_REMINDER
            }
            val requestCode = eventId.hashCode()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {}
    }
}
