package com.example.update

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.BuildConfig
import com.example.MainActivity
import com.example.calendar.PersianCalendarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BackgroundUpdateCheckReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_BACKGROUND_UPDATE = "com.example.ACTION_CHECK_BACKGROUND_UPDATE"
        private const val CHANNEL_ID = "app_new_release_channel"
        private const val NOTIFICATION_ID = 8899
        private const val REQUEST_CODE_ALARM = 8871
        private const val REQUEST_CODE_NOTIFICATION = 8872
        private const val TAG = "BackgroundUpdateCheck"

        /**
         * Schedules periodic daily check in the background (around 12:00 PM).
         * Runs even if the app is closed, and persists across device reboots.
         */
        fun schedulePeriodicCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!now.before(target)) {
                // If 12:00 has already passed today, schedule for 12:00 tomorrow
                target.add(Calendar.DAY_OF_YEAR, 1)
            }

            val triggerMs = target.timeInMillis

            val intent = Intent(context, BackgroundUpdateCheckReceiver::class.java).apply {
                action = ACTION_CHECK_BACKGROUND_UPDATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
                }
                Log.d(TAG, "Periodic background update check scheduled for: ${target.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling background update check: ${e.message}", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BackgroundUpdateCheckReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-arm periodic schedule after reboot or app upgrade
            schedulePeriodicCheck(context)

            // Also re-arm 18:00 reminder if there was a pending update not installed yet
            val pending = AppUpdateManager.getPendingUpdateInfo(context)
            if (pending != null && pending.isAvailable) {
                UpdateReminderReceiver.scheduleReminder(context, pending)
            }
            return
        }

        if (action == ACTION_CHECK_BACKGROUND_UPDATE) {
            // Re-schedule for next day first
            schedulePeriodicCheck(context)

            // Only perform network request if internet is available
            if (!isOnline(context)) {
                Log.d(TAG, "No internet connection available for background update check")
                return
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val update = AppUpdateManager.checkForUpdates(context, isManual = true)
                    if (update != null && update.isAvailable) {
                        Log.i(TAG, "New release found in background: v${update.latestVersion}")
                        showNewReleaseNotification(context, update)
                        // Also arm the 18:00 reminder in case this notification is dismissed/ignored
                        UpdateReminderReceiver.scheduleReminder(context, update)
                    } else {
                        Log.d(TAG, "Background update check completed: app is up-to-date")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Background update check error: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val net = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(net) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                @Suppress("DEPRECATION")
                val info = cm.activeNetworkInfo
                info != null && info.isConnected
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun showNewReleaseNotification(context: Context, updateInfo: UpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "انتشار نسخه جدید تقویم",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اطلاع‌رسانی به کاربر هنگام انتشار نسخه جدید در گیت‌هاب"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(UpdateReminderReceiver.EXTRA_FROM_UPDATE_REMINDER, true)
            putExtra(UpdateReminderReceiver.EXTRA_VERSION, updateInfo.latestVersion)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_NOTIFICATION,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val verFa = PersianCalendarHelper.toPersianDigits(updateInfo.latestVersion)
        val title = "🎉 نسخه جدید تقویم منتشر شد! (نسخه $verFa)"

        // Extract first line of changelog or fallback message
        val firstLine = updateInfo.releaseNotes.lines().firstOrNull { it.isNotBlank() }
        val message = if (!firstLine.isNullOrBlank()) {
            "امکانات جدید: $firstLine - برای مشاهده و ارتقا لمس کنید."
        } else {
            "نسخه $verFa با امکانات و بهبودهای جدید هم‌اکنون در دسترس است."
        }

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
                "مشاهده و به‌روزرسانی",
                pendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.i(TAG, "New release notification posted for v${updateInfo.latestVersion}")
    }
}
