package com.example.alarm

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

class EventAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EVENT_REMINDER = "com.example.ACTION_EVENT_REMINDER"
        private const val CHANNEL_ID = "hamgam_event_alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_EVENT_REMINDER) return

        val title = intent.getStringExtra("event_title") ?: "یادآور رویداد"
        val message = intent.getStringExtra("event_desc") ?: "زمان فرا رسیدن رویداد ثبت‌شده در تقویم"
        val eventId = intent.getStringExtra("event_id") ?: "0"

        // Wake screen
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "Hamgam:EventAlarmWakeLock"
        )
        wakeLock?.acquire(30000L) // 30 seconds

        showEventAlarmNotification(context, title, message, eventId.hashCode())
    }

    private fun showEventAlarmNotification(context: Context, title: String, message: String, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "زنگ و یادآورهای رویدادها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدار و بیدارباش رویدادهای تقویم همگام"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 1000)
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
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 800, 300, 800, 300, 1000))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
