package com.example.alarm

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

class NoteReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NOTE_REMINDER = "com.example.ACTION_NOTE_REMINDER"
        const val EXTRA_OPEN_NOTE_ID = "open_note_id"
        private const val CHANNEL_ID = "hamgam_note_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_NOTE_REMINDER) return

        val noteId = intent.getStringExtra("note_id") ?: return
        val title = intent.getStringExtra("note_title")?.takeIf { it.isNotBlank() } ?: "یادداشت"
        val snippet = intent.getStringExtra("note_snippet") ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآورهای یادداشت‌ها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "یادآوری زمان‌دار برای یادداشت‌های همگام"
                enableVibration(true)
                setSound(soundUri, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTE_ID, noteId)
        }
        val notificationId = ("note_" + noteId).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 $title")
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snippet))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
