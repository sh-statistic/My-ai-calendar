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

object EventAlarmScheduler {

    fun scheduleEventAlarm(context: Context, event: EventEntity) {
        if (event.startTime.isBlank() || event.isDeleted) return

        // Parse reminder option from description
        val reminderMatch = Regex("\\[یادآوری:([^\\]]+)\\]").find(event.description)
        val reminderStr = reminderMatch?.groupValues?.getOrNull(1)?.trim() ?: "همزمان با رویداد"
        if (reminderStr == "بدون زنگ") return

        val offsetMinutes = when (reminderStr) {
            "۵ دقیقه قبل" -> 5
            "۱۰ دقیقه قبل" -> 10
            "۱۵ دقیقه قبل" -> 15
            "۳۰ دقیقه قبل" -> 30
            "۱ ساعت قبل" -> 60
            "۱ روز قبل" -> 1440
            else -> 0 // همزمان با رویداد
        }

        try {
            // Convert Persian date to Gregorian
            val pParts = event.persianDate.split("/").mapNotNull { it.toIntOrNull() }
            if (pParts.size != 3) return
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
            if (triggerMs <= System.currentTimeMillis()) {
                // Event time is in the past
                return
            }

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
            Log.d("EventAlarmScheduler", "Alarm scheduled for event '${event.title}' at ${cal.time}")
        } catch (e: Exception) {
            Log.e("EventAlarmScheduler", "Error scheduling alarm: ${e.message}")
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
