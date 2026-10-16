package com.example.prayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FajrAlarmService : Service() {

    companion object {
        const val ACTION_START_ALARM = "com.example.prayer.ACTION_START_FAJR_ALARM"
        const val ACTION_STOP_ALARM = "com.example.prayer.ACTION_STOP_FAJR_ALARM"
        private const val CHANNEL_ID = "hamgam_fajr_continuous_alarm_channel"
        private const val NOTIFICATION_ID = 9902
        private const val AUTO_STOP_DELAY_MS = 20 * 60 * 1000L // 20 minutes safety timeout

        @Volatile
        var isRinging: Boolean = false
            private set

        fun stopAlarm(context: Context) {
            val intent = Intent(context, FajrAlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable {
        stopAlarmAndSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_ALARM) {
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_ALARM) {
            val cityName = intent.getStringExtra("cityName") ?: "شهر شما"
            val offsetMinutes = intent.getIntExtra("offsetMinutes", 5)

            acquireWakeLock()
            startContinuousRingtone()
            startContinuousVibration()

            val notification = buildAlarmNotification(cityName, offsetMinutes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isRinging = true
            com.example.data.AppLogger.log(
                this,
                "اذان صبح",
                "پخش زنگ ممتد و لرزش بیدارباش اذان صبح آغاز شد ($cityName)"
            )

            // Schedule safety auto-stop after 20 minutes to prevent battery drain
            handler.removeCallbacks(autoStopRunnable)
            handler.postDelayed(autoStopRunnable, AUTO_STOP_DELAY_MS)
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Hamgam:FajrAlarmServiceWakeLock"
            )?.apply {
                acquire(AUTO_STOP_DELAY_MS)
            }
        }
    }

    private fun startContinuousRingtone() {
        if (mediaPlayer != null) return

        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(audioAttributes)
                isLooping = true // پشت سر هم و بدون وقفه پخش می‌شود
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (_: Exception) {
            // Fallback: try default ringtone if alarm sound fails
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, ringtoneUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) {}
        }
    }

    private fun startContinuousVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // repeat at index 0 makes it repeat indefinitely
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun buildAlarmNotification(cityName: String, offsetMinutes: Int): android.app.Notification {
        createNotificationChannel()

        val stopIntent = Intent(this, FajrAlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            102,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (offsetMinutes > 0) {
            "$offsetMinutes دقیقه تا اذان صبح به افق $cityName باقی مانده است. وقت بیداری و مناجات سحرگاهی!"
        } else {
            "وقت اذان صبح به افق $cityName فرارسیده است. التماس دعا!"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ زنگ بیدارباش هوشمند اذان صبح")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true) // کاربر نمی‌تواند نوتیفیکیشن را سوایپ کند
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "⛔ قطع زنگ بیدارباش",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "زنگ مداوم بیدارباش اذان صبح",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "پخش مداوم زنگ بیدارباش تا زمان قطع توسط کاربر"
                // No sound in notification itself because MediaPlayer is playing it in a continuous loop
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun stopAlarmAndSelf() {
        handler.removeCallbacks(autoStopRunnable)
        if (isRinging) {
            com.example.data.AppLogger.log(this, "اذان صبح", "زنگ بیدارباش اذان صبح متوقف شد")
        }
        isRinging = false

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        stopAlarmAndSelf()
        super.onDestroy()
    }
}
