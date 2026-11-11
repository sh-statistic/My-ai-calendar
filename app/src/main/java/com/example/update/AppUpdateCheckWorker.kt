package com.example.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.calendar.PersianCalendarHelper
import com.example.settings.AppPreferences
import com.example.ui.components.extractLocalizedNotes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppUpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "AppUpdateCheckWorker"
        const val CHANNEL_ID = "app_new_release_channel"
        const val NOTIFICATION_ID = 8899
        private const val REQUEST_CODE_NOTIFICATION = 8872
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "AppUpdateCheckWorker started background update inquiry...")

            val update = AppUpdateManager.checkForUpdates(applicationContext, isManual = true)
            if (update != null && update.isAvailable) {
                Log.i(TAG, "New release found in background work: v${update.latestVersion}")
                showNewReleaseNotification(applicationContext, update)

                // Arm the 18:00 reminder in case this notification is dismissed or missed
                UpdateReminderReceiver.scheduleReminder(applicationContext, update)
            } else {
                Log.d(TAG, "Background update check completed: app is already up-to-date.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background update check encountered an error: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun showNewReleaseNotification(context: Context, updateInfo: UpdateInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val currentLang = AppPreferences.getAppLanguage(context)
        val isFa = currentLang == "fa"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isFa) "انتشار نسخه جدید تقویم" else "New Calendar Version",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isFa) "اطلاع‌رسانی هنگام انتشار نسخه جدید در گیت‌هاب" else "Notifications for new releases on GitHub"
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
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

        val verText = if (isFa) PersianCalendarHelper.toPersianDigits(updateInfo.latestVersion) else updateInfo.latestVersion
        val title = if (isFa) "🎉 نسخه جدید تقویم منتشر شد! (نسخه $verText)" else "🎉 New Calendar Version Available! (v$verText)"

        // Parse localized changelog snippet for the notification body
        val localizedNotes = extractLocalizedNotes(updateInfo.releaseNotes, isFa)
        val firstLine = localizedNotes.lines().firstOrNull { it.isNotBlank() }?.trim()

        val message = if (!firstLine.isNullOrBlank()) {
            if (isFa) "امکانات جدید: $firstLine - برای مشاهده و ارتقا لمس کنید."
            else "New features: $firstLine - Tap to view & update."
        } else {
            if (isFa) "نسخه $verText با امکانات و بهبودهای جدید هم‌اکنون در دسترس است."
            else "Version $verText with improvements is now ready for installation."
        }

        val buttonActionText = if (isFa) "مشاهده و به‌روزرسانی" else "View & Update"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download_done,
                buttonActionText,
                pendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.i(TAG, "High-priority update notification posted successfully for v${updateInfo.latestVersion}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }
}
