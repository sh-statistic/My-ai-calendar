package com.example.announce

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One announcement the developer published from Telegram. */
data class Announcement(
    val id: String,
    val text: String,
    val options: List<String>
)

/**
 * Talks to the announcement side of the feedback relay: the app polls the
 * latest announcement and posts votes back. No account, no token — the
 * announcement is public broadcast content, and the vote path carries the
 * same unguessable segment as the feedback path.
 */
object AnnouncementClient {

    private const val LATEST_URL = "https://feedback.sh2103.shop/a/latest"
    private const val VOTE_URL = "https://feedback.sh2103.shop/f/7pytoxb6dr1e-vote"
    const val MAX_OPTIONS = 3

    /** Pure parser (unit-tested): relay JSON to an Announcement, or null on junk. */
    fun parseLatest(json: String): Announcement? {
        return try {
            val root = JSONObject(json)
            val ann = root.optJSONObject("announcement") ?: return null
            val id = ann.optString("id")
            val text = ann.optString("text")
            if (id.isBlank() || text.isBlank()) return null
            val options = mutableListOf<String>()
            val arr = ann.optJSONArray("options")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val option = arr.optString(i).trim()
                    if (option.isNotEmpty()) options.add(option)
                }
            }
            Announcement(id, text, options.take(MAX_OPTIONS))
        } catch (e: Exception) {
            null
        }
    }

    /** Outcome of one poll of the relay: a failure is worth a retry, "no announcement" is not. */
    sealed class LatestResult {
        data class Ok(val announcement: Announcement?) : LatestResult()
        object Failed : LatestResult()
    }

    suspend fun fetchLatest(): LatestResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(LATEST_URL).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            when {
                conn.responseCode in 200..299 ->
                    LatestResult.Ok(parseLatest(conn.inputStream.bufferedReader().readText()))
                conn.responseCode in 500..599 -> LatestResult.Failed
                else -> LatestResult.Ok(null)
            }
        } catch (e: Exception) {
            LatestResult.Failed
        }
    }

    /** True when the relay accepted and tallied the vote. */
    suspend fun vote(id: String, option: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("id", id)
                .put("option", option)
                .toString()
            val conn = URL(VOTE_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) {
                false
            } else {
                JSONObject(conn.inputStream.bufferedReader().readText()).optBoolean("ok", false)
            }
        } catch (e: Exception) {
            false
        }
    }
}
