package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/sh-statistic/My-ai-calendar/releases/latest"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
    private const val KEY_SAFETY_BACKUP_JSON = "safety_backup_json"
    private const val KEY_SAFETY_BACKUP_TIME = "safety_backup_timestamp"
    
    // Check interval: 7 days in milliseconds
    const val CHECK_INTERVAL_MILLIS = 30L * 24 * 60 * 60 * 1000

    /**
     * Checks GitHub for the latest release.
     * @param isManual true if triggered by user click (bypasses 7-day interval check)
     */
    suspend fun checkForUpdates(
        context: Context,
        isManual: Boolean = false
    ): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()

        if (!isManual && (now - lastCheck) < CHECK_INTERVAL_MILLIS) {
            Log.d(TAG, "Update check skipped (checked within last 7 days)")
            return@withContext null
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "My-AI-Calendar-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
                val releaseNotes = json.optString("body", "تغییرات و بهبودهای جدید").trim()
                val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()

                // Find APK in assets
                var downloadUrl = ""
                var apkName = "MyCalendar-v$tagName.apk"
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            apkName = name
                            break
                        }
                    }
                }

                // If no asset is attached, fallback to html_url
                if (downloadUrl.isBlank()) {
                    downloadUrl = json.optString("html_url", "")
                }

                val hasUpdate = isNewerVersion(tagName, currentVersion)

                // Update last checked timestamp
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

                UpdateInfo(
                    isAvailable = hasUpdate,
                    latestVersion = tagName,
                    currentVersion = currentVersion,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    apkName = apkName
                )
            } else {
                Log.w(TAG, "GitHub API returned code: $responseCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares semantic versions (e.g. "0.6.0" vs "0.5.0")
     */
    fun isNewerVersion(remoteVer: String, currentVer: String): Boolean {
        if (remoteVer.isBlank() || currentVer.isBlank()) return false
        val remoteParts = remoteVer.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVer.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    /**
     * Creates an immediate safety backup in multiple persistent storage locations before upgrading.
     */
    fun performSafetyBackup(context: Context, backupJson: String): Boolean {
        if (backupJson.isBlank()) return false
        try {
            // 1. Save in SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SAFETY_BACKUP_JSON, backupJson)
                .putLong(KEY_SAFETY_BACKUP_TIME, System.currentTimeMillis())
                .apply()

            // 2. Save in app external files directory
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val file = File(extDir, "safety_backup.json")
                file.writeText(backupJson)
            }

            // 3. Save in public Downloads directory so it survives app uninstall
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val publicFile = File(downloadsDir, "MyCalendar_SafetyBackup.json")
                    publicFile.writeText(backupJson)
                }
            } catch (ignored: Exception) {
                // Secondary fallback, silent ignore
            }

            Log.i(TAG, "Safety backup successfully saved before update!")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create safety backup: ${e.message}", e)
            return false
        }
    }

    /**
     * Retrieves the latest safety backup JSON if available.
     */
    fun getLatestSafetyBackup(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromPrefs = prefs.getString(KEY_SAFETY_BACKUP_JSON, null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs

        val extDir = context.getExternalFilesDir(null)
        val file = File(extDir, "safety_backup.json")
        if (file.exists() && file.length() > 0) {
            return try { file.readText() } catch (_: Exception) { null }
        }

        val publicFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "MyCalendar_SafetyBackup.json")
        if (publicFile.exists() && publicFile.length() > 0) {
            return try { publicFile.readText() } catch (_: Exception) { null }
        }

        return null
    }

    /**
     * Clears the safety backup flag after successful restore
     */
    fun clearSafetyBackup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SAFETY_BACKUP_JSON).apply()
    }

    /**
     * Initiates the download of the APK file using Android's DownloadManager.
     */
    fun startDownload(context: Context, downloadUrl: String, apkName: String): Long {
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("دانلود به‌روزرسانی تقویم")
            setDescription("در حال دانلود نسخه جدید برنامه...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    /**
     * Opens the installer prompt for the downloaded APK using FileProvider.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Log.e(TAG, "APK file does not exist: ${apkFile.absolutePath}")
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
