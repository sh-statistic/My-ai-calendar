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
import android.media.Ringtone
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
import com.example.data.AppLogger

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
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
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
            val offsetMinutes = intent.getIntExtra("offsetMinutes", 0)

            acquireWakeLock()
            ensureAlarmVolumeIsAdequate()
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
            AppLogger.log(
                this,
                "اذان صبح",
                "پخش زنگ ممتد و لرزش بیدارباش اذان صبح با کانال آلارم آغاز شد ($cityName)"
            )

            // Auto-stop safety timeout
            handler.removeCallbacks(autoStopRunnable)
            handler.postDelayed(autoStopRunnable, AUTO_STOP_DELAY_MS)
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Hamgam:FajrAlarmServiceWakeLock"
            )?.apply {
                acquire(AUTO_STOP_DELAY_MS)
            }
        }
    }

    private fun ensureAlarmVolumeIsAdequate() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            // Ensure alarm stream volume is at least 70% of max volume so it is audible
            val minTargetVol = ((maxVol * 0.7f).toInt()).coerceAtLeast(1)
            if (currentVol < minTargetVol) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, minTargetVol, 0)
            }
        } catch (_: Exception) {}
    }

    private fun startContinuousRingtone() {
        if (mediaPlayer != null || fallbackRingtone != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()

        val alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        var playerInitialized = false

        if (alarmUri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setAudioAttributes(audioAttributes)
                    setDataSource(applicationContext, alarmUri)
                    isLooping = true
                    setVolume(1.0f, 1.0f)
                    prepare()
                    start()
                }
                playerInitialized = true
            } catch (e: Exception) {
                AppLogger.log(this, "اذان صبح", "خطا در بارگذاری مدیاپلیر با آدرس آلارم: ${e.message}", isSuccess = false)
            }
        }

        // Secondary fallback: Try default ringtone with MediaPlayer
        if (!playerInitialized) {
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (ringtoneUri != null) {
                    mediaPlayer = MediaPlayer().apply {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_ALARM)
                        setAudioAttributes(audioAttributes)
                        setDataSource(applicationContext, ringtoneUri)
                        isLooping = true
                        setVolume(1.0f, 1.0f)
                        prepare()
                        start()
                    }
                    playerInitialized = true
                }
            } catch (e: Exception) {
                AppLogger.log(this, "اذان صبح", "خطا در فال‌بک مدیاپلیر: ${e.message}", isSuccess = false)
            }
        }

        // Tertiary fallback: Use RingtoneManager direct playback on STREAM_ALARM
        if (!playerInitialized) {
            try {
                val targetUri = alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (targetUri != null) {
                    fallbackRingtone = RingtoneManager.getRingtone(applicationContext, targetUri)?.apply {
                        @Suppress("DEPRECATION")
                        streamType = AudioManager.STREAM_ALARM
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            this.audioAttributes = audioAttributes
                        }
                        play()
                    }
                }
            } catch (e: Exception) {
                AppLogger.log(this, "اذان صبح", "خطا در اجرای فال‌بک رینگتون: ${e.message}", isSuccess = false)
            }
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
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator?.vibrate(effect, audioAttributes)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0, audioAttributes)
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

        // Launch full-screen FajrAlarmActivity
        val fullScreenIntent = Intent(this, FajrAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("cityName", cityName)
            putExtra("offsetMinutes", offsetMinutes)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            102,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = when {
            offsetMinutes > 0 -> "$offsetMinutes دقیقه تا اذان صبح به افق $cityName باقی مانده است. وقت بیداری و مناجات سحرگاهی!"
            offsetMinutes < 0 -> {
                val after = -offsetMinutes
                if (after == 60) "۱ ساعت از اذان صبح به افق $cityName گذشته است. وقت بیداری و شروع روز!"
                else "$after دقیقه از اذان صبح به افق $cityName گذشته است. وقت بیداری و شروع روز!"
            }
            else -> "وقت اذان صبح به افق $cityName فرارسیده است. التماس دعا!"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ زنگ بیدارباش هوشمند اذان صبح")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
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
            AppLogger.log(this, "اذان صبح", "زنگ بیدارباش اذان صبح متوقف شد")
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
            if (fallbackRingtone?.isPlaying == true) {
                fallbackRingtone?.stop()
            }
            fallbackRingtone = null
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
