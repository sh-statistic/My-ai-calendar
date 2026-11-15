package com.example.announce

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.settings.AppPreferences

/**
 * Shows an announcement as a heads-up notification: the developer's text
 * with one action button per option. Tapping the body opens the app;
 * tapping an option casts the vote (see AnnouncementVoteReceiver).
 */
object AnnouncementNotifier {

    const val CHANNEL_ID = "announcements_channel"
    // PendingIntent request codes and notification ids are derived per
    // announcement (see below) from a high range: 9911/9912 belong to the
    // update reminder, and several announcements can sit in the shade at
    // the same time without overwriting or cross-wiring each other.

    /** Stable per-announcement notification id, so announcements stack instead of replacing each other. */
    fun notificationIdFor(id: String): Int = 100_000 + (id.hashCode() and 0x3FFFFF)

    private fun openRequestCodeFor(id: String): Int = 3_000_000 + (id.hashCode() and 0xFFFFF)

    private fun voteRequestCodeFor(id: String, index: Int): Int =
        5_000_000 + (id.hashCode() and 0xFFFFF) * 4 + index

    fun show(context: Context, announcement: Announcement) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as? NotificationManager ?: return
        val isFa = AppPreferences.getAppLanguage(context) == "fa"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isFa) "اعلان‌های برنامه" else "App announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isFa) "پیام‌ها و نظرسنجی‌های سازنده برنامه" else "Messages and polls from the app developer"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            openRequestCodeFor(announcement.id),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(if (isFa) "\u200F📣 پیام تازه از برنامه" else "📣 New message from the app")
            .setContentText(rtl(announcement.text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(rtl(announcement.text)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        announcement.options.forEachIndexed { index, label ->
            val voteIntent = Intent(context, AnnouncementVoteReceiver::class.java).apply {
                action = AnnouncementVoteReceiver.ACTION_VOTE
                putExtra(AnnouncementVoteReceiver.EXTRA_ANN_ID, announcement.id)
                putExtra(AnnouncementVoteReceiver.EXTRA_OPTION, index)
            }
            val votePendingIntent = PendingIntent.getBroadcast(
                context,
                voteRequestCodeFor(announcement.id, index),
                voteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(thumbIconFor(label), label, votePendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationIdFor(announcement.id), builder.build())
        } catch (e: SecurityException) {
            // Notification permission vanished between the check and the
            // post; the caller has already marked it seen.
        }
    }

    /**
     * True when a notification can actually reach the user right now:
     * the app's notifications are enabled and this channel is not muted.
     * The check worker only counts an announcement as seen when this is
     * true, so an announcement fetched while notifications were off is
     * not lost — it is shown by a later check once they are on.
     */
    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return false
            val channel = manager.getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                return false
            }
        }
        return true
    }

    /** Right-to-left mark first, so Persian text renders right-aligned. */
    private fun rtl(text: String): String = "\u200F" + text

    /** Thumb icons for like/dislike options; other labels are text-only. */
    private fun thumbIconFor(label: String): Int = when {
        label.contains("دیسلایک") || label.contains("👎") -> R.drawable.ic_thumb_down
        label.contains("لایک") || label.contains("👍") -> R.drawable.ic_thumb_up
        else -> 0
    }
}
