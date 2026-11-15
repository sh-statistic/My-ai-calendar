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
    // The 993x block is this feature's own: 9911/9912 already belong to the
    // update reminder's alarm and content PendingIntents, and sharing a
    // request code would merge the two notifications' tap intents.
    const val NOTIFICATION_ID = 9931
    private const val REQUEST_CODE_OPEN = 9932
    private const val REQUEST_CODE_VOTE_BASE = 9940

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
            REQUEST_CODE_OPEN,
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
                REQUEST_CODE_VOTE_BASE + index,
                voteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(thumbIconFor(label), label, votePendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // Notification permission not granted; the announcement is still
            // marked seen by the caller, matching the update notifier's stance.
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
