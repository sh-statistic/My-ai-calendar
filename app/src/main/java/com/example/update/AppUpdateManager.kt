package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
     * Checks if the app is allowed to install unknown apps (Android 8.0+ / API 26+)
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens the system settings to allow this app to install unknown apps
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening install settings: ${e.message}", e)
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try { context.startActivity(fallbackIntent) } catch (_: Exception) {}
            }
        }
    }

    /**
     * Target file location for the downloaded update APK in app's private external/internal storage
     */
    fun getUpdateApkFile(context: Context, apkName: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "updates")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, apkName)
    }

    /**
     * Checks if an APK file has already been downloaded and is valid (> 500 KB)
     */
    fun getDownloadedApkIfReady(context: Context, apkName: String): File? {
        val file = getUpdateApkFile(context, apkName)
        return if (file.exists() && file.length() > 500_000) file else null
    }

    /**
     * Downloads the APK directly with live progress tracking and automatic GitHub 30x redirect following.
     */
    suspend fun downloadApkWithProgress(
        context: Context,
        downloadUrl: String,
        apkName: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null
        try {
            val targetFile = getUpdateApkFile(context, apkName)
            val tempFile = File(targetFile.parentFile, "$apkName.tmp")
            if (tempFile.exists()) tempFile.delete()

            var currentUrl = downloadUrl
            var redirects = 0
            val maxRedirects = 7

            // Follow HTTP/HTTPS redirects (GitHub Releases redirect to AWS S3 CDN)
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "My-AI-Calendar-Android-App")
                    setRequestProperty("Accept", "application/octet-stream, application/vnd.android.package-archive, */*")
                }

                val status = connection.responseCode
                if (status in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("آدرس تغییر مسیر (Redirect) دریافت نشد"))
                    }
                    currentUrl = newUrl
                    redirects++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    return@withContext Result.failure(Exception("خطای سرور دانلود: کد $status"))
                }
            }

            val totalBytes = connection!!.contentLengthLong
            inputStream = connection.inputStream
            outputStream = FileOutputStream(tempFile)

            val buffer = ByteArray(8 * 1024)
            var downloadedBytes = 0L
            var lastProgressUpdateTime = 0L

            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastProgressUpdateTime > 80 || downloadedBytes == totalBytes) {
                    lastProgressUpdateTime = now
                    val progress = if (totalBytes > 0) {
                        (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else {
                        -1f
                    }
                    withContext(Dispatchers.Main) {
                        onProgress(progress, downloadedBytes, totalBytes)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            if (tempFile.length() < 100_000) {
                tempFile.delete()
                return@withContext Result.failure(Exception("فایل دانلود شده ناقص است"))
            }

            // Move temp file to final target APK
            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            withContext(Dispatchers.Main) {
                onProgress(1f, targetFile.length(), targetFile.length())
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    /**
     * Opens the installer prompt for the downloaded APK using FileProvider.
     * Returns true if installer intent was successfully dispatched.
     */
    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) {
            Log.e(TAG, "APK file does not exist or empty: ${apkFile.absolutePath}")
            return false
        }

        return try {
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
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch installer intent: ${e.message}", e)
            false
        }
    }

    /**
     * Initiates the download of the APK file using Android's DownloadManager (Alternative/Legacy).
     */
    fun startDownload(context: Context, downloadUrl: String, apkName: String): Long {
        val targetFile = getUpdateApkFile(context, apkName)
        if (targetFile.exists()) targetFile.delete()

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("دانلود به‌روزرسانی تقویم")
            setDescription("در حال دانلود نسخه جدید برنامه...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, apkName)
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }
}
