package com.example.calendar

import android.content.Context
import com.example.update.GitHubTrust
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/**
 * Silent background download of the yearly official-occasions files.
 *
 * Files live on the repo's `occasions-data` branch (one JSON per Jalali
 * year, at the branch root) and are fetched from the fixed raw address.
 * Publishing a new year is a single commit to that branch — no app update.
 *
 * Design rules (spec: occasions-download-spec.md):
 * - A year is fetched at most once per 7 days; every failure is silent and
 *   leaves the current data untouched.
 * - A download is stored only after it fully validates; the write goes to
 *   a temp file and is renamed into place, then the year's in-memory cache
 *   in [PersianOccasionsHelper] is invalidated so the new data is picked up.
 * - The connection mirrors AppUpdateManager: 10 s timeouts and the
 *   GitHubTrust socket factory. Certificate checking is never disabled.
 */
object OccasionsDownloader {

    private const val BASE_URL =
        "https://raw.githubusercontent.com/sh-statistic/My-ai-calendar/occasions-data"
    private const val PREFS = "OccasionsRemote"
    private const val THROTTLE_MS = 7L * 24 * 60 * 60 * 1000

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "occasions-downloader").apply { isDaemon = true }
    }

    fun urlFor(year: Int): String = "$BASE_URL/occasions_$year.json"

    /** Where the downloaded copy of a year's file lives on the device. */
    fun localFile(context: Context, year: Int): File =
        File(File(context.filesDir, "occasions"), "occasions_$year.json")

    /**
     * Considers a background refresh for [years]. Called whenever a year is
     * loaded; cheap when throttled. Never blocks the caller.
     */
    fun maybeRefresh(context: Context, years: Collection<Int>) {
        try {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val due = years.distinct().filter { year ->
                now - prefs.getLong("check_$year", 0L) >= THROTTLE_MS
            }
            if (due.isEmpty()) return
            // Mark the attempt up-front so failures also respect the throttle.
            prefs.edit().apply {
                for (year in due) putLong("check_$year", now)
            }.apply()
            executor.execute {
                for (year in due) fetchOne(appContext, year)
            }
        } catch (e: Exception) {
            // Silent by design.
        }
    }

    private fun fetchOne(context: Context, year: Int) {
        try {
            val text = download(context, urlFor(year)) ?: return
            if (!validate(text, year)) return
            val target = localFile(context, year)
            val dir = target.parentFile ?: return
            dir.mkdirs()
            val tmp = File(dir, target.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(target)) {
                tmp.delete()
                return
            }
            PersianOccasionsHelper.invalidateYearCache(year)
        } catch (e: Exception) {
            // Silent by design: any failure leaves current data untouched.
        }
    }

    private fun download(context: Context, urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(urlString).openConnection() as HttpURLConnection
            if (conn is HttpsURLConnection) {
                GitHubTrust.socketFactory(context)?.let { conn.sslSocketFactory = it }
            }
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            if (conn.responseCode != HttpURLConnection.HTTP_OK) null
            else conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Full structural validation of a candidate yearly file. Pure logic
     * (apart from JSON parsing), so it is unit-tested directly.
     */
    fun validate(jsonText: String, year: Int): Boolean {
        return try {
            val root = JSONObject(jsonText)
            if (root.length() != 12) return false
            var prev: Triple<Int, Int, Int>? = null
            for (month in 1..12) {
                val daysObj = root.optJSONObject(month.toString()) ?: return false
                val expectedDays = when {
                    month <= 6 -> 31
                    month <= 11 -> 30
                    else -> if (HijriCalendarUtils.isPersianLeapYear(year)) 30 else 29
                }
                if (daysObj.length() != expectedDays) return false
                for (day in 1..expectedDays) {
                    val d = daysObj.optJSONObject(day.toString()) ?: return false
                    val h = Triple(d.getInt("hYear"), d.getInt("hMonth"), d.getInt("hDay"))
                    if (h.first !in 1300..1700 || h.second !in 1..12 || h.third !in 1..30) return false
                    d.getBoolean("isHoliday") // throws on a wrong type
                    val occ = d.getJSONArray("occasions")
                    for (i in 0 until occ.length()) occ.getString(i)
                    prev?.let { p ->
                        val nextDay = h.first == p.first && h.second == p.second && h.third == p.third + 1
                        val nextMonth = h.third == 1 && h.first == p.first && h.second == p.second + 1
                        val nextYear = h.third == 1 && h.second == 1 && h.first == p.first + 1
                        if (!nextDay && !nextMonth && !nextYear) return false
                    }
                    prev = h
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
