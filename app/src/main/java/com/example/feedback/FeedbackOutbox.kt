package com.example.feedback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * On-device outbox for user feedback (Sadra's design, 2026-11-15): tapping
 * Send never waits on the network. The feedback is queued here and a
 * WorkManager job delivers it in the background, retrying whenever the
 * network is available until the relay confirms delivery. Queued items are
 * delivered oldest-first, and an item leaves the queue only after a
 * confirmed delivery — nothing is lost to a dead relay, a closed app or a
 * rebooted phone.
 */
object FeedbackOutbox {

    data class PendingFeedback(
        val id: String,
        val text: String,
        val contact: String,
        val appVersion: String,
        val device: String,
        val photoFileName: String?,
        val crashLog: String? = null
    )

    private const val PREFS = "feedback_outbox"
    private const val KEY_ITEMS = "items"
    private const val DIR_NAME = "feedback_outbox"
    private const val WORK_NAME = "feedback_flush"

    // ---- Pure JSON codec (unit-tested) ----

    fun encodeItems(items: List<PendingFeedback>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("text", item.text)
            obj.put("contact", item.contact)
            obj.put("appVersion", item.appVersion)
            obj.put("device", item.device)
            if (item.photoFileName != null) obj.put("photoFileName", item.photoFileName)
            if (item.crashLog != null) obj.put("crashLog", item.crashLog)
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeItems(raw: String?): List<PendingFeedback> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val out = ArrayList<PendingFeedback>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isBlank()) continue
                out.add(
                    PendingFeedback(
                        id = id,
                        text = obj.optString("text"),
                        contact = obj.optString("contact"),
                        appVersion = obj.optString("appVersion"),
                        device = obj.optString("device"),
                        photoFileName = obj.optString("photoFileName").ifBlank { null },
                        crashLog = obj.optString("crashLog").ifBlank { null }
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---- Storage ----

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun outboxDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    @Synchronized
    fun loadAll(context: Context): List<PendingFeedback> =
        decodeItems(prefs(context).getString(KEY_ITEMS, null))

    @Synchronized
    private fun saveAll(context: Context, items: List<PendingFeedback>) {
        // commit(), not apply(): a queued feedback must survive even a
        // process kill right after the tap — losing it silently would break
        // the outbox's whole promise. The file is tiny, the cost is nothing.
        prefs(context).edit().putString(KEY_ITEMS, encodeItems(items)).commit()
    }

    fun pendingCount(context: Context): Int = loadAll(context).size

    /**
     * Queues a feedback and returns the stored entry. Photo bytes are copied
     * into the outbox folder so the photo survives until delivery, exactly
     * like the text.
     */
    @Synchronized
    fun enqueue(
        context: Context,
        text: String,
        contact: String,
        photoJpeg: ByteArray?,
        crashLog: String? = null
    ): PendingFeedback {
        val id = UUID.randomUUID().toString()
        var photoFileName: String? = null
        if (photoJpeg != null) {
            val name = "$id.jpg"
            try {
                File(outboxDir(context), name).writeBytes(photoJpeg)
                photoFileName = name
            } catch (e: Exception) {
                photoFileName = null
            }
        }
        val item = PendingFeedback(
            id = id,
            text = text,
            contact = contact,
            appVersion = FeedbackSender.appVersionName(context),
            device = FeedbackSender.deviceLabel(),
            photoFileName = photoFileName,
            crashLog = crashLog
        )
        saveAll(context, loadAll(context) + item)
        return item
    }

    /** Removes one delivered entry and its photo file. */
    @Synchronized
    fun remove(context: Context, id: String) {
        val items = loadAll(context)
        val removed = items.firstOrNull { it.id == id } ?: return
        saveAll(context, items.filterNot { it.id == id })
        removed.photoFileName?.let { name ->
            try {
                File(outboxDir(context), name).delete()
            } catch (e: Exception) {
                // A leftover photo file is harmless; the entry itself is gone.
            }
        }
    }

    fun photoBytes(context: Context, item: PendingFeedback): ByteArray? {
        val name = item.photoFileName ?: return null
        return try {
            val file = File(outboxDir(context), name)
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delivers queued feedbacks oldest-first. An entry is removed only after
     * [sendOne] confirms delivery; the first failure stops the walk so the
     * remaining entries keep their order for the next attempt. Returns how
     * many were delivered.
     */
    suspend fun flush(
        context: Context,
        sendOne: suspend (PendingFeedback, ByteArray?) -> Boolean
    ): Int {
        var sent = 0
        while (true) {
            val next = loadAll(context).firstOrNull() ?: break
            val ok = try {
                sendOne(next, photoBytes(context, next))
            } catch (e: Exception) {
                false
            }
            if (!ok) break
            remove(context, next.id)
            sent++
        }
        return sent
    }

    // ---- Delivery scheduling & connectivity ----

    /**
     * Arms the background flush. WorkManager keeps the request across app
     * closures and reboots and only runs it while a network is connected;
     * the worker itself retries with backoff while entries remain.
     */
    fun scheduleFlush(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<FeedbackFlushWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        } catch (e: Exception) {
            // The entry stays queued; the next enqueue or send re-arms the job.
        }
    }

    /** True when the phone currently has an internet-capable network. */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        return try {
            val network = cm.activeNetwork
            if (network != null) {
                cm.getNetworkCapabilities(network)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            } else {
                @Suppress("DEPRECATION")
                cm.activeNetworkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            false
        }
    }
}
