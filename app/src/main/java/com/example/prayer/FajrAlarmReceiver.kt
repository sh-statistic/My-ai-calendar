package com.example.prayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FajrAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FAJR_ALARM = "com.example.ACTION_FAJR_ALARM"
        private const val CHANNEL_ID = "hamgam_fajr_alarm_channel"
        private const val NOTIFICATION_ID = 9901
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action

        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Reschedule alarm after device restart
            FajrAlarmManager.scheduleFajrAlarm(context)
            return
        }

        if (action == ACTION_FAJR_ALARM) {
            val cityName = intent.getStringExtra("cityName") ?: "شهر شما"
            val offsetMinutes = intent.getIntExtra("offsetMinutes", 5)

            com.example.data.AppLogger.log(
                context,
                "اذان صبح",
                "زمان بیدارباش فرارسید (شهر: $cityName). ارسال درخواست اجرای سرویس زنگ ممتد..."
            )

            // Start foreground continuous alarm service with looping ringtone
            val serviceIntent = Intent(context, FajrAlarmService::class.java).apply {
                this.action = FajrAlarmService.ACTION_START_ALARM
                putExtra("cityName", cityName)
                putExtra("offsetMinutes", offsetMinutes)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (e: Exception) {
                com.example.data.AppLogger.log(
                    context,
                    "اذان صبح",
                    "محدودیت اجرای سرویس پس‌زمینه؛ پخش زنگ از طریق اعلان سیستم: ${e.message}",
                    isSuccess = false
                )
                // Fallback to simple notification if background service start fails
                showAlarmNotification(context, cityName, offsetMinutes)
            }

            // Automatically reschedule tomorrow's alarm dynamically
            FajrAlarmManager.scheduleFajrAlarm(context)
        }
    }

    private fun showAlarmNotification(context: Context, cityName: String, offsetMinutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Create notification channel for Android O+ with strict ALARM audio attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدار و بیدارباش اذان صبح",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری و بیدارباش هوشمند اذان صبح"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 400, 1000, 400, 1500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (offsetMinutes > 0) {
            "$offsetMinutes دقیقه تا اذان صبح در $cityName باقی مانده است. وقت بیداری و مناجات سحرگاهی!"
        } else {
            "وقت اذان صبح به افق $cityName فرارسیده است. التماس دعا!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ بیدارباش هوشمند اذان صبح")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 400, 1000, 400, 1500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
