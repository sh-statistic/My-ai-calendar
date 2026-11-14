package com.example.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
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
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/sh-statistic/My-ai-calendar/releases?per_page=30"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/sh-statistic/My-ai-calendar/releases/latest"
    private const val PREFS_NAME = "app_update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
    private const val KEY_SAFETY_BACKUP_JSON = "safety_backup_json"
    private const val KEY_SAFETY_BACKUP_TIME = "safety_backup_timestamp"

    // Pending Update Keys
    private const val KEY_PENDING_APK_PATH = "pending_install_apk_path"
    private const val KEY_PENDING_VERSION = "pending_update_version"
    private const val KEY_PENDING_NOTES = "pending_update_notes"
    private const val KEY_PENDING_URL = "pending_update_url"
    private const val KEY_PENDING_APK_NAME = "pending_update_apk_name"
    private const val KEY_LAST_KNOWN_VERSION = "last_known_installed_version"
    private const val KEY_SAFETY_BACKUP_RESTORED_TIME = "safety_backup_restored_timestamp"
    private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_update_version"
    private const val KEY_SNOOZED_VERSION = "snoozed_update_version"
    private const val KEY_SNOOZE_UNTIL = "snoozed_update_until"

    // فاصله بررسی خودکار گیت‌هاب هنگام باز شدن برنامه: ۱۲ ساعت
    const val CHECK_INTERVAL_MILLIS = 12L * 60 * 60 * 1000

    /**
     * آیا برای این نسخه قبلاً اعلان «نسخه جدید منتشر شد» داده شده است؟ (هر نسخه فقط یک بار اعلان)
     */
    fun shouldNotifyForVersion(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_NOTIFIED_VERSION, null) != version
    }

    fun markVersionNotified(context: Context, version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_NOTIFIED_VERSION, version).apply()
    }

    /**
     * با زدن «بعداً»، پنجره آپدیت همین نسخه تا شروع روز بعد دوباره به‌صورت خودکار نمایش داده نمی‌شود.
     */
    fun snoozeUpdateDialog(context: Context, version: String) {
        val tomorrow = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SNOOZED_VERSION, version)
            .putLong(KEY_SNOOZE_UNTIL, tomorrow.timeInMillis)
            .apply()
    }

    fun isUpdateDialogSnoozed(context: Context, version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snoozedVersion = prefs.getString(KEY_SNOOZED_VERSION, null) ?: return false
        val until = prefs.getLong(KEY_SNOOZE_UNTIL, 0L)
        return snoozedVersion == version && System.currentTimeMillis() < until
    }

    /**
     * اتصال HTTPS به گیت‌هاب با اعتبارسنجی کامل گواهی.
     * روی اندرویدهای قدیمی (۶ و ۷.۰) که گواهی‌های ریشه جدید را ندارند، گواهی‌های ریشه داخل برنامه
     * ([GitHubTrust]) به مخزن سیستم اضافه می‌شوند؛ هیچ‌گاه بررسی گواهی غیرفعال نمی‌شود.
     */
    private fun openGitHubConnection(context: Context, url: URL): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        if (conn is HttpsURLConnection) {
            GitHubTrust.socketFactory(context)?.let { conn.sslSocketFactory = it }
        }
        return conn
    }

    /**
     * تبدیل خطاهای فنی شبکه/SSL به پیام قابل فهم برای کاربر.
     */
    private fun friendlyNetworkError(e: Throwable): String = when (e) {
        is SSLException ->
            "اتصال امن به سرور گیت‌هاب برقرار نشد (خطای گواهی SSL). تاریخ و ساعت گوشی را بررسی کنید یا از طریق مرورگر دانلود نمایید."
        is java.net.UnknownHostException ->
            "سرور گیت‌هاب در دسترس نیست؛ اتصال اینترنت را بررسی کنید."
        is java.net.SocketTimeoutException ->
            "زمان اتصال به سرور دانلود به پایان رسید؛ دوباره تلاش کنید."
        else -> e.message ?: "خطا در برقراری ارتباط با سرور دانلود"
    }

    /**
     * Dynamically resolves the currently installed app version name from Android PackageManager.
     * Fallbacks to BuildConfig.VERSION_NAME if PackageManager query fails.
     */
    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName
            if (!vName.isNullOrBlank()) {
                vName.removePrefix("v").removePrefix("V").trim()
            } else {
                BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME.removePrefix("v").removePrefix("V").trim()
        }
    }

    /**
     * Checks GitHub for updates.
     * Always targets the latest stable release directly (skipping any intermediate versions)
     * and aggregates changelogs from the user's current version up to the latest release.
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
            Log.d(TAG, "Update check skipped (checked within interval)")
            return@withContext null
        }

        val currentVersion = getInstalledVersionName(context)

        // 1. Try fetching full releases list to aggregate all changelogs from current to latest
        var updateInfo = fetchFromReleasesList(context, currentVersion)

        // 2. Fallback to latest release endpoint if full list fails
        if (updateInfo == null) {
            Log.d(TAG, "Releases list fetch failed, falling back to latest release endpoint")
            updateInfo = fetchFromLatestEndpoint(context, currentVersion)
        }

        if (updateInfo != null) {
            prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()
            if (updateInfo.isAvailable) {
                savePendingUpdateInfo(context, updateInfo)
            }
        }

        updateInfo
    }

    /**
     * Fetches all releases to skip intermediate versions and aggregate full changelogs.
     */
    private fun fetchFromReleasesList(context: Context, currentVersion: String): UpdateInfo? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_RELEASES_URL)
            connection = openGitHubConnection(context, url).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "My-AI-Calendar-Android-App")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val releasesArray = org.json.JSONArray(response)
            if (releasesArray.length() == 0) return null

            // Filter stable releases (non-draft and non-prerelease)
            val stableReleases = mutableListOf<JSONObject>()
            for (i in 0 until releasesArray.length()) {
                val rel = releasesArray.getJSONObject(i)
                val isDraft = rel.optBoolean("draft", false)
                val isPrerelease = rel.optBoolean("prerelease", false)
                if (!isDraft && !isPrerelease) {
                    stableReleases.add(rel)
                }
            }

            if (stableReleases.isEmpty()) return null

            // The first stable release is the latest available release
            val latestRelease = stableReleases[0]
            val latestTagName = latestRelease.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
            if (latestTagName.isBlank()) return null

            val hasUpdate = isNewerVersion(latestTagName, currentVersion)
            if (!hasUpdate) {
                return UpdateInfo(
                    isAvailable = false,
                    latestVersion = latestTagName,
                    currentVersion = currentVersion,
                    releaseNotes = "",
                    downloadUrl = "",
                    apkName = ""
                )
            }

            // Find APK in assets of the latest release (keep versioned local filename to prevent version collisions)
            var downloadUrl = ""
            val apkName = "MyCalendar-v$latestTagName.apk"
            val assets = latestRelease.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }
            if (downloadUrl.isBlank()) {
                downloadUrl = latestRelease.optString("html_url", "")
            }

            // Aggregate changelog for all versions newer than currentVersion
            val newerReleases = mutableListOf<Pair<String, String>>()
            for (rel in stableReleases) {
                val tag = rel.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
                if (isNewerVersion(tag, currentVersion)) {
                    val body = rel.optString("body", "").trim()
                    newerReleases.add(Pair(tag, body))
                }
            }

            val aggregatedNotes = when {
                newerReleases.size <= 1 -> {
                    latestRelease.optString("body", "تغییرات و بهبودهای جدید").trim()
                }
                else -> {
                    buildString {
                        newerReleases.forEachIndexed { index, (tag, body) ->
                            if (index > 0) append("\n\n───────────────\n\n")
                            append("✨ تغییرات نسخه ${com.example.calendar.PersianCalendarHelper.toPersianDigits(tag)}:\n")
                            append(if (body.isNotBlank()) body else "تغییرات و بهبودهای جدید")
                        }
                    }
                }
            }

            return UpdateInfo(
                isAvailable = true,
                latestVersion = latestTagName,
                currentVersion = currentVersion,
                releaseNotes = aggregatedNotes,
                downloadUrl = downloadUrl,
                apkName = apkName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchFromReleasesList: ${e.message}", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fallback to GitHub latest release endpoint if listing fails.
     */
    private fun fetchFromLatestEndpoint(context: Context, currentVersion: String): UpdateInfo? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_LATEST_RELEASE_URL)
            connection = openGitHubConnection(context, url).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "My-AI-Calendar-Android-App")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return null
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "").removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "تغییرات و بهبودهای جدید").trim()

            var downloadUrl = ""
            val apkName = "MyCalendar-v$tagName.apk"
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (downloadUrl.isBlank()) {
                downloadUrl = json.optString("html_url", "")
            }

            val hasUpdate = isNewerVersion(tagName, currentVersion)

            return UpdateInfo(
                isAvailable = hasUpdate,
                latestVersion = tagName,
                currentVersion = currentVersion,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                apkName = apkName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchFromLatestEndpoint: ${e.message}", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Pending install APK file state tracking.
     */
    fun setPendingInstallApkPath(context: Context, path: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PENDING_APK_PATH, path).apply()
    }

    fun getPendingInstallApk(context: Context): File? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_PENDING_APK_PATH, null) ?: return null
        val file = File(path)
        return if (file.exists() && file.length() > 500_000) file else null
    }

    fun clearPendingInstallApk(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PENDING_APK_PATH).apply()
    }

    /**
     * Pending update information persistence for smart 18:00 reminder.
     */
    fun savePendingUpdateInfo(context: Context, updateInfo: UpdateInfo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PENDING_VERSION, updateInfo.latestVersion)
            .putString(KEY_PENDING_NOTES, updateInfo.releaseNotes)
            .putString(KEY_PENDING_URL, updateInfo.downloadUrl)
            .putString(KEY_PENDING_APK_NAME, updateInfo.apkName)
            .apply()
    }

    fun getPendingUpdateInfo(context: Context): UpdateInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val version = prefs.getString(KEY_PENDING_VERSION, null) ?: return null
        val currentVersion = getInstalledVersionName(context)
        val hasUpdate = isNewerVersion(version, currentVersion)
        return UpdateInfo(
            isAvailable = hasUpdate,
            latestVersion = version,
            currentVersion = currentVersion,
            releaseNotes = prefs.getString(KEY_PENDING_NOTES, "") ?: "",
            downloadUrl = prefs.getString(KEY_PENDING_URL, "") ?: "",
            apkName = prefs.getString(KEY_PENDING_APK_NAME, "MyCalendar-v$version.apk") ?: "MyCalendar-v$version.apk"
        )
    }

    fun clearPendingUpdateInfo(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PENDING_VERSION)
            .remove(KEY_PENDING_NOTES)
            .remove(KEY_PENDING_URL)
            .remove(KEY_PENDING_APK_NAME)
            .apply()
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
     * بررسی می‌کند آیا رشته JSON پشتیبان واقعاً حاوی حداقل یک رویداد، کار، یادداشت یا خاطره عمر هست یا خیر.
     * این بررسی مانع از آن می‌شود که یک برنامه تازه نصب‌شده و خالی، فایل پشتیبان قبلی کاربر را با آرایه‌های خالی بازنویسی کند.
     */
    fun hasMeaningfulBackupContent(backupJson: String?): Boolean {
        if (backupJson.isNullOrBlank()) return false
        return try {
            val root = JSONObject(backupJson)
            val eventsLen = root.optJSONArray("events")?.length() ?: 0
            val tasksLen = root.optJSONArray("tasks")?.length() ?: 0
            val notesLen = root.optJSONArray("notes")?.length() ?: 0
            val memento = root.optJSONObject("mementoMori")
            val mementoNotesLen = memento?.optJSONObject("reflectionNotes")?.length() ?: 0
            val mementoConfigured = memento?.optBoolean("isConfigured", false) ?: false
            eventsLen > 0 || tasksLen > 0 || notesLen > 0 || mementoNotesLen > 0 || mementoConfigured
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates an immediate safety backup in multiple persistent storage locations before upgrading.
     */
    fun performSafetyBackup(context: Context, backupJson: String): Boolean {
        if (backupJson.isBlank()) return false
        val isMeaningful = hasMeaningfulBackupContent(backupJson)
        try {
            // 1. Save in SharedPreferences (do not overwrite an existing non-empty backup with an empty one)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingPrefsBackup = prefs.getString(KEY_SAFETY_BACKUP_JSON, null)
            if (isMeaningful || !hasMeaningfulBackupContent(existingPrefsBackup)) {
                prefs.edit()
                    .putString(KEY_SAFETY_BACKUP_JSON, backupJson)
                    .putLong(KEY_SAFETY_BACKUP_TIME, System.currentTimeMillis())
                    .apply()
            }

            // 2. Save in app external files directory
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val file = File(extDir, "safety_backup.json")
                val existingExt = if (file.exists()) try { file.readText() } catch (_: Exception) { null } else null
                if (isMeaningful || !hasMeaningfulBackupContent(existingExt)) {
                    file.writeText(backupJson)
                }
            }

            // 3. Save in public Downloads directory so it survives app uninstall (only when backup has real data)
            if (isMeaningful) {
                try {
                    val fileName = "MyCalendar_SafetyBackup.json"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        // Check if an entry with this name already exists in Downloads
                        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val projection = arrayOf(MediaStore.Downloads._ID)
                        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                        val selectionArgs = arrayOf(fileName)
                        var existingUri: Uri? = null
                        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                                existingUri = android.content.ContentUris.withAppendedId(collection, id)
                            }
                        }

                        val targetUri = existingUri ?: run {
                            val values = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            }
                            resolver.insert(collection, values)
                        }

                        if (targetUri != null) {
                            resolver.openOutputStream(targetUri, "wt")?.use { out ->
                                out.write(backupJson.toByteArray(Charsets.UTF_8))
                                out.flush()
                            }
                        } else {
                            Log.w(TAG, "Failed to create or resolve MediaStore URI for $fileName")
                        }
                    } else {
                        if (!hasStoragePermissionForBackup(context)) {
                            Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission not granted on API ${Build.VERSION.SDK_INT}; public Downloads write may fail")
                        }
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                            val publicFile = File(downloadsDir, fileName)
                            publicFile.writeText(backupJson)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing safety backup to Downloads: ${e.message}", e)
                }
            }

            Log.i(TAG, "Safety backup successfully saved before update! (hasData=$isMeaningful)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create safety backup: ${e.message}", e)
            return false
        }
    }

    /**
     * File-based variant of [performSafetyBackup]: copies an already-written backup file
     * into the same persistent locations without ever loading its content into memory.
     * The export pipeline produces a File (not a String) so a media-heavy backup of tens
     * of megabytes never becomes one giant in-memory String on the UI path, and never
     * lands in SharedPreferences either. Restore detection still works because
     * [getLatestSafetyBackup] falls back to the external file and the public Downloads copy.
     */
    fun performSafetyBackupFromFile(context: Context, backupFile: File): Boolean {
        if (!backupFile.exists() || backupFile.length() == 0L) return false
        val isMeaningful = fileHasMeaningfulBackupContent(backupFile)
        return try {
            // 1. Save in app external files directory
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val file = File(extDir, "safety_backup.json")
                if (isMeaningful || !fileHasMeaningfulBackupContent(file)) {
                    backupFile.inputStream().use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }
                }
            }

            // 2. Save in public Downloads directory so it survives app uninstall (only when backup has real data)
            if (isMeaningful) {
                try {
                    val fileName = "MyCalendar_SafetyBackup.json"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        val projection = arrayOf(MediaStore.Downloads._ID)
                        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                        val selectionArgs = arrayOf(fileName)
                        var existingUri: Uri? = null
                        resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                                existingUri = android.content.ContentUris.withAppendedId(collection, id)
                            }
                        }
                        val targetUri = existingUri ?: run {
                            val values = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            }
                            resolver.insert(collection, values)
                        }
                        if (targetUri != null) {
                            resolver.openOutputStream(targetUri, "wt")?.use { out ->
                                backupFile.inputStream().use { input -> input.copyTo(out) }
                            }
                        } else {
                            Log.w(TAG, "Failed to create or resolve MediaStore URI for $fileName")
                        }
                    } else {
                        if (!hasStoragePermissionForBackup(context)) {
                            Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission not granted on API ${Build.VERSION.SDK_INT}; public Downloads write may fail")
                        }
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                            val publicFile = File(downloadsDir, fileName)
                            backupFile.inputStream().use { input ->
                                publicFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing safety backup file to Downloads: ${e.message}", e)
                }
            }

            Log.i(TAG, "Safety backup (from file) successfully saved! (hasData=$isMeaningful)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create safety backup from file: ${e.message}", e)
            false
        }
    }

    private fun fileHasMeaningfulBackupContent(file: File): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        // A backup larger than 2 MB certainly carries real content; never parse it whole here.
        if (file.length() > 2_000_000L) return true
        return try { hasMeaningfulBackupContent(file.readText()) } catch (_: Exception) { false }
    }

    private val safetyBackupFileNameRegex = Regex("""^MyCalendar_SafetyBackup( \(\d+\))?\.json$""")

    /**
     * Finds every public Downloads safety backup candidate, newest first. Besides the exact
     * name (MyCalendar_SafetyBackup.json), repeated saves through the system file picker
     * create numbered variants (MyCalendar_SafetyBackup (1).json, ...) which must be
     * detected as well. Each entry is a Uri (API 29+, via MediaStore) or a File (legacy).
     */
    private fun findPublicSafetyBackupCandidates(context: Context): List<Pair<Uri?, File?>> {
        val found = mutableListOf<Triple<Long, Uri?, File?>>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Downloads._ID,
                    MediaStore.Downloads.DISPLAY_NAME,
                    MediaStore.Downloads.DATE_MODIFIED
                )
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("MyCalendar_SafetyBackup%")
                context.contentResolver.query(
                    collection, projection, selection, selectionArgs,
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                    val modIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIdx) ?: continue
                        if (!safetyBackupFileNameRegex.matches(name)) continue
                        val uri = android.content.ContentUris.withAppendedId(collection, cursor.getLong(idIdx))
                        found.add(Triple(cursor.getLong(modIdx), uri, null))
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore safety backup scan failed: ${e.message}")
            }
        }
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.listFiles()?.forEach { f ->
                if (f.isFile && safetyBackupFileNameRegex.matches(f.name)) {
                    found.add(Triple(f.lastModified() / 1000, null, f))
                }
            }
        } catch (_: Exception) { }
        return found.sortedByDescending { it.first }.map { it.second to it.third }
    }

    private fun readPublicSafetyBackupCandidate(context: Context, candidate: Pair<Uri?, File?>): String? {
        val (uri, file) = candidate
        return try {
            when {
                uri != null -> context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                file != null -> file.readText()
                else -> null
            }
        } catch (_: Exception) { null }
    }

    /**
     * Retrieves the latest non-empty safety backup JSON if available and directly readable.
     */
    fun getLatestSafetyBackup(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromPrefs = prefs.getString(KEY_SAFETY_BACKUP_JSON, null)
        if (hasMeaningfulBackupContent(fromPrefs)) return fromPrefs

        val extDir = context.getExternalFilesDir(null)
        val file = File(extDir, "safety_backup.json")
        if (file.exists() && file.length() > 0) {
            val extText = try { file.readText() } catch (_: Exception) { null }
            if (hasMeaningfulBackupContent(extText)) return extText
        }

        for (candidate in findPublicSafetyBackupCandidates(context)) {
            val pubText = readPublicSafetyBackupCandidate(context, candidate)
            if (hasMeaningfulBackupContent(pubText)) return pubText
        }

        return null
    }

    /**
     * بررسی می‌کند آیا فایل پشتیبان MyCalendar_SafetyBackup.json (یا نسخه‌های شماره‌دار آن) در پوشه Downloads وجود دارد،
     * اما به دلیل محدودیت امنیتی اندروید ۱۰+ (Scoped Storage پس از حذف و نصب مجدد برنامه)
     * نیاز است که کاربر آن را از طریق فایل‌پیکر انتخاب کند.
     */
    fun hasUnreadablePublicSafetyBackup(context: Context): Boolean {
        return try {
            val candidates = findPublicSafetyBackupCandidates(context)
            if (candidates.isEmpty()) return false
            candidates.none { !readPublicSafetyBackupCandidate(context, it).isNullOrBlank() }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if there is an unconsumed safety backup saved right before an in-app update.
     */
    fun hasPendingSafetyBackupInPrefs(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_SAFETY_BACKUP_JSON, null).isNullOrBlank()
    }

    /**
     * Clears the safety backup flag after successful restore or post-update verification.
     */
    fun clearSafetyBackup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_SAFETY_BACKUP_JSON)
            .putLong(KEY_SAFETY_BACKUP_RESTORED_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * بررسی می‌کند آیا برنامه به نسخه جدیدتری ارتقا یافته است یا خیر.
     * در صورت ارتقا، فایل‌های APK قدیمی را پاکسازی کرده و نام نسخه جدید را برمی‌گرداند.
     */
    fun checkAndConsumeVersionUpgrade(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentVersion = getInstalledVersionName(context)
        val lastKnownVersion = prefs.getString(KEY_LAST_KNOWN_VERSION, null)
        val pendingVersion = prefs.getString(KEY_PENDING_VERSION, null)
        val hadSafetyBackup = !prefs.getString(KEY_SAFETY_BACKUP_JSON, null).isNullOrBlank()

        val upgraded = when {
            lastKnownVersion != null && isNewerVersion(currentVersion, lastKnownVersion) -> true
            lastKnownVersion == null && pendingVersion != null && !isNewerVersion(pendingVersion, currentVersion) -> true
            lastKnownVersion == null && hadSafetyBackup -> true
            else -> false
        }

        if (lastKnownVersion != currentVersion) {
            prefs.edit().putString(KEY_LAST_KNOWN_VERSION, currentVersion).apply()
        }

        if (upgraded || (pendingVersion != null && !isNewerVersion(pendingVersion, currentVersion))) {
            clearPendingUpdateInfo(context)
            clearPendingInstallApk(context)
            cleanupOldUpdateApks(context)
            // پاک کردن هر دو اعلان آپدیت (یادآور ۱۸:۰۰ و اعلان انتشار نسخه) و لغو آلارم یادآور
            UpdateReminderReceiver.cancelReminder(context)
        } else {
            cleanupOldUpdateApks(context)
        }

        return if (upgraded) currentVersion else null
    }

    /**
     * پاکسازی خودکار فایل‌های APK قدیمی یا موقت (.tmp) از پوشه دانلود داخلی برنامه
     * پس از نصب موفقیت‌آمیز نسخه جدید جهت آزادسازی فضای ذخیره‌سازی و جلوگیری از تداخل نسخه‌ها.
     */
    fun cleanupOldUpdateApks(context: Context) {
        try {
            val currentVersion = getInstalledVersionName(context)
            val dirs = listOfNotNull(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                File(context.filesDir, "updates")
            )
            for (dir in dirs) {
                if (!dir.exists()) continue
                val files = dir.listFiles() ?: continue
                for (f in files) {
                    if (f.name.endsWith(".tmp", ignoreCase = true)) {
                        f.delete()
                        continue
                    }
                    if (f.name.endsWith(".apk", ignoreCase = true)) {
                        val apkVersion = getApkArchiveVersionName(context, f)
                        if (apkVersion == null || !isNewerVersion(apkVersion, currentVersion)) {
                            Log.i(TAG, "Deleting old/installed update APK: ${f.name} (version=$apkVersion)")
                            f.delete()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cleanup old update APKs: ${e.message}")
        }
    }

    private fun getApkArchiveVersionName(context: Context, apkFile: File): String? {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageArchiveInfo(
                    apkFile.absolutePath,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            }
            pInfo?.versionName?.removePrefix("v")?.removePrefix("V")?.trim()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * بررسی می‌کند آیا برای نوشتن فایل در Downloads مجوز لازم است و داده شده است یا خیر.
     * روی اندروید ۶ تا ۹ (API 23 تا 28) نیازمند WRITE_EXTERNAL_STORAGE است.
     * روی اندروید ۱۰ به بالا (Scoped Storage با MediaStore) و اندروید ۵ به پایین نیازی به درخواست زمان اجرا نیست.
     */
    fun hasStoragePermissionForBackup(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT in Build.VERSION_CODES.M..Build.VERSION_CODES.P) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
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
     * Checks if an APK file has already been downloaded, is valid (> 500 KB),
     * and actually contains a newer version than the currently installed version.
     */
    fun getDownloadedApkIfReady(context: Context, apkName: String): File? {
        val file = getUpdateApkFile(context, apkName)
        if (!file.exists() || file.length() <= 500_000) return null
        val currentVersion = getInstalledVersionName(context)
        val archiveVersion = getApkArchiveVersionName(context, file)
        if (archiveVersion == null || !isNewerVersion(archiveVersion, currentVersion)) {
            file.delete()
            return null
        }
        return file
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
            var reachedContent = false

            // Follow HTTP/HTTPS redirects manually so each hop receives headers and SSL config
            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                if (!url.protocol.equals("https", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("آدرس دانلود ناامن است (فقط HTTPS مجاز است)"))
                }
                connection = openGitHubConnection(context, url).apply {
                    instanceFollowRedirects = false
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
                    currentUrl = URL(url, newUrl).toString()
                    redirects++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    reachedContent = true
                    break
                } else {
                    return@withContext Result.failure(Exception("خطای سرور دانلود: کد $status"))
                }
            }
            if (!reachedContent) {
                return@withContext Result.failure(Exception("تعداد تغییر مسیرهای (Redirect) سرور دانلود بیش از حد مجاز است"))
            }

            // نکته: getContentLengthLong() از API 24 به بعد وجود دارد و روی اندروید ۶ باعث NoSuchMethodError می‌شود
            val totalBytes = connection!!.getHeaderField("Content-Length")?.trim()?.toLongOrNull() ?: -1L
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

            if (tempFile.length() < 100_000 || (totalBytes > 0 && tempFile.length() != totalBytes)) {
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
            Result.failure(Exception(friendlyNetworkError(e), e))
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
     *
     * هشدار: DownloadManager از مخزن گواهی سیستم استفاده می‌کند و گواهی‌های ریشه داخلی ([GitHubTrust]) را
     * نمی‌شناسد؛ بنابراین روی اندروید ۶ و ۷.۰ دانلود از گیت‌هاب با آن شکست می‌خورد. از [downloadApkWithProgress] استفاده شود.
     */
    @Deprecated("روی اندرویدهای قدیمی با خطای گواهی شکست می‌خورد؛ از downloadApkWithProgress استفاده کنید")
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
