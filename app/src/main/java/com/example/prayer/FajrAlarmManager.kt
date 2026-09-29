package com.example.prayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import java.util.TimeZone

data class FajrAlarmSettings(
    val isEnabled: Boolean = false,
    val cityName: String = "تهران",
    val offsetMinutesBefore: Int = 5, // Default 5 minutes before Fajr
    val nextAlarmTimestamp: Long = 0L
)

object FajrAlarmManager {

    private const val PREFS_NAME = "hamgam_prayer_prefs"
    private const val KEY_ENABLED = "fajr_alarm_enabled"
    private const val KEY_CITY = "fajr_alarm_city"
    private const val KEY_OFFSET = "fajr_alarm_offset_minutes"
    private const val KEY_NEXT_TIMESTAMP = "fajr_alarm_next_timestamp"
    private const val ALARM_REQUEST_CODE = 8801

    fun getSettings(context: Context): FajrAlarmSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FajrAlarmSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            cityName = prefs.getString(KEY_CITY, "تهران") ?: "تهران",
            offsetMinutesBefore = prefs.getInt(KEY_OFFSET, 5),
            nextAlarmTimestamp = prefs.getLong(KEY_NEXT_TIMESTAMP, 0L)
        )
    }

    fun saveSettings(context: Context, settings: FajrAlarmSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putString(KEY_CITY, settings.cityName)
            .putInt(KEY_OFFSET, settings.offsetMinutesBefore)
            .putLong(KEY_NEXT_TIMESTAMP, settings.nextAlarmTimestamp)
            .apply()

        if (settings.isEnabled) {
            scheduleFajrAlarm(context)
        } else {
            cancelFajrAlarm(context)
        }
    }

    fun scheduleFajrAlarm(context: Context) {
        val settings = getSettings(context)
        if (!settings.isEnabled) return

        val city = PrayerTimesCalculator.getCityByName(settings.cityName)
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH) + 1
        val currentDay = now.get(Calendar.DAY_OF_MONTH)

        val timesToday = PrayerTimesCalculator.calculatePrayerTimes(
            city.latitude, city.longitude, currentYear, currentMonth, currentDay
        )

        // Convert Fajr time into Calendar instance
        val todayFajrTotalMinutes = (timesToday.fajrHour * 60.0).toInt()
        val alarmMinutesToday = todayFajrTotalMinutes - settings.offsetMinutesBefore

        val alarmCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarmMinutesToday / 60)
            set(Calendar.MINUTE, alarmMinutesToday % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If today's alarm time has already passed, schedule for tomorrow
        if (alarmCal.timeInMillis <= System.currentTimeMillis()) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
            val timesTomorrow = PrayerTimesCalculator.calculatePrayerTimes(
                city.latitude, city.longitude,
                tomorrow.get(Calendar.YEAR),
                tomorrow.get(Calendar.MONTH) + 1,
                tomorrow.get(Calendar.DAY_OF_MONTH)
            )
            val tomorrowFajrTotalMinutes = (timesTomorrow.fajrHour * 60.0).toInt()
            val alarmMinutesTomorrow = tomorrowFajrTotalMinutes - settings.offsetMinutesBefore

            alarmCal.timeInMillis = tomorrow.timeInMillis
            alarmCal.set(Calendar.HOUR_OF_DAY, alarmMinutesTomorrow / 60)
            alarmCal.set(Calendar.MINUTE, alarmMinutesTomorrow % 60)
            alarmCal.set(Calendar.SECOND, 0)
            alarmCal.set(Calendar.MILLISECOND, 0)
        }

        val triggerTimeMs = alarmCal.timeInMillis

        // Update stored next timestamp
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_NEXT_TIMESTAMP, triggerTimeMs).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FajrAlarmReceiver::class.java).apply {
            action = FajrAlarmReceiver.ACTION_FAJR_ALARM
            putExtra("cityName", settings.cityName)
            putExtra("offsetMinutes", settings.offsetMinutesBefore)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.d("FajrAlarmManager", "Fajr alarm scheduled for ${alarmCal.time}")
        } catch (e: Exception) {
            Log.e("FajrAlarmManager", "Failed to schedule exact alarm: ${e.message}")
        }
    }

    fun cancelFajrAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FajrAlarmReceiver::class.java).apply {
            action = FajrAlarmReceiver.ACTION_FAJR_ALARM
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
        alarmManager.cancel(pendingIntent)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_NEXT_TIMESTAMP, 0L).apply()
    }
}
