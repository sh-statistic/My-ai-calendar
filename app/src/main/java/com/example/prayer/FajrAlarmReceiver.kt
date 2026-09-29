package com.example.prayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

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

            showAlarmNotification(context, cityName, offsetMinutes)

            // Automatically reschedule tomorrow's alarm dynamically
            FajrAlarmManager.scheduleFajrAlarm(context)
        }
    }

    private fun showAlarmNotification(context: Context, cityName: String, offsetMinutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "هشدار اذان صبح همگام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری و بیدارباش هوشمند اذان صبح"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
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

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val message = if (offsetMinutes > 0) {
            "$offsetMinutes دقیقه تا اذان صبح در $cityName باقی مانده است. روز پربرکتی داشته باشید!"
        } else {
            "وقت اذان صبح به افق $cityName فرارسیده است. التماس دعا!"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ بیدارباش هوشمند اذان صبح")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
