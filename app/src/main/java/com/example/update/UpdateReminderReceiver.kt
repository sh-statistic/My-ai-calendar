package com.example.update

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.calendar.PersianCalendarHelper
import java.util.Calendar

class UpdateReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPDATE_REMINDER = "com.example.ACTION_UPDATE_REMINDER"
        const val EXTRA_VERSION = "extra_update_version"
        const val EXTRA_FROM_UPDATE_REMINDER = "extra_from_update_reminder"
        const val CHANNEL_ID = "app_update_reminder_channel"
        const val NOTIFICATION_ID = 8842
        const val REQUEST_CODE_ALARM = 9911
        const val REQUEST_CODE_NOTIFICATION = 9912

        private const val TAG = "UpdateReminderReceiver"

        /**
         * Schedules exact reminder for 18:00 (today if before 18:00, or tomorrow if after).
         */
        fun scheduleReminder(context: Context, updateInfo: UpdateInfo) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            // Persist pending update info
            AppUpdateManager.savePendingUpdateInfo(context, updateInfo)

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!now.before(target)) {
                // If it's 18:00 or later, schedule for 18:00 tomorrow
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val triggerMs = target.timeInMillis

            val intent = Intent(context, UpdateReminderReceiver::class.java).apply {
                action = ACTION_UPDATE_REMINDER
                putExtra(EXTRA_VERSION, updateInfo.latestVersion)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                        } else {
                            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                        }
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                    }
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
                Log.d(TAG, "Update reminder scheduled for: ${target.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling update reminder: ${e.message}", e)
            }
        }

        /**
         * Cancels scheduled reminder and dismisses notification once updated.
         */
        fun cancelReminder(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                val intent = Intent(context, UpdateReminderReceiver::class.java).apply {
                    action = ACTION_UPDATE_REMINDER
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_ALARM,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager?.cancel(pendingIntent)

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(NOTIFICATION_ID)

                AppUpdateManager.clearPendingUpdateInfo(context)
                Log.d(TAG, "Update reminder cancelled")
            } catch (_: Exception) {}
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_UPDATE_REMINDER) return

        val pendingInfo = AppUpdateManager.getPendingUpdateInfo(context)
        val currentVer = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
        val targetVer = intent.getStringExtra(EXTRA_VERSION) ?: pendingInfo?.latestVersion

        // If app has already been updated to or past target version, abort reminder
        if (targetVer == null || !AppUpdateManager.isNewerVersion(targetVer, currentVer)) {
            Log.d(TAG, "App is already up-to-date ($currentVer >= $targetVer). Skipping reminder.")
            AppUpdateManager.clearPendingUpdateInfo(context)
            return
        }

        showUpdateNotification(context, targetVer)

        // Reschedule for next day 18:00 in case user still doesn't install it
        if (pendingInfo != null) {
            scheduleReminder(context, pendingInfo)
        }
    }

    private fun showUpdateNotification(context: Context, targetVersion: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری به‌روزرسانی تقویم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری نسخه جدید تقویم در صورت عدم نصب"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_UPDATE_REMINDER, true)
            putExtra(EXTRA_VERSION, targetVersion)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_NOTIFICATION,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "نسخه جدید تقویم هنوز نصب نشده!"
        val message = "نسخه ${PersianCalendarHelper.toPersianDigits(targetVersion)} با تغییرات و بهبودهای جدید آماده نصب است."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download_done,
                "مشاهده و نصب",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "Update reminder notification posted for version $targetVersion")
    }
}
