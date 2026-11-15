package com.example.feedback

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the app's own technical crash report — and nothing else (Sadra's
 * design, 2026-11-15). When the app crashes, the report (when, which
 * version, which phone, the error and its stack trace) is written to a
 * small file. It never leaves the phone on its own: the feedback form
 * shows it is attached, the user can remove it, and it rides along only
 * with a feedback the user chooses to send. No user data — no notes,
 * tasks, events or usage history — is ever part of the report.
 */
object CrashLogStore {

    const val MAX_REPORT_CHARS = 8000

    private const val DIR_NAME = "crash_reports"
    private const val FILE_NAME = "last_crash.txt"

    /** Pure report builder (unit-tested): header + full stack trace, head kept. */
    fun buildReport(
        timestampMillis: Long,
        appVersion: String,
        device: String,
        threadName: String,
        throwable: Throwable
    ): String {
        val trace = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date(timestampMillis))
        val full = buildString {
            append("Crash report — Hamgam calendar\n")
            append("time: ").append(stamp).append('\n')
            append("appVersion: ").append(appVersion).append('\n')
            append("device: ").append(device).append('\n')
            append("thread: ").append(threadName).append('\n')
            append("---\n")
            append(trace)
        }
        return if (full.length <= MAX_REPORT_CHARS) full
        else full.take(MAX_REPORT_CHARS) + "\n…(truncated)"
    }

    private fun reportFile(context: Context): File =
        File(File(context.filesDir, DIR_NAME).apply { mkdirs() }, FILE_NAME)

    /**
     * Registers the crash writer. The previous handler still runs after the
     * report is written, so the crash itself behaves exactly as before.
     */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val report = buildReport(
                    System.currentTimeMillis(),
                    FeedbackSender.appVersionName(appContext),
                    FeedbackSender.deviceLabel(),
                    thread.name,
                    throwable
                )
                reportFile(appContext).writeText(report)
            } catch (e: Exception) {
                // Never let the reporter itself change how the crash ends.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** The stored crash report, or null when the app has not crashed. */
    fun peek(context: Context): String? = try {
        val file = reportFile(context)
        if (file.exists()) file.readText().ifBlank { null } else null
    } catch (e: Exception) {
        null
    }

    /**
     * Automatic reporting (Sadra's request, 2026-11-15): moves the stored
     * report into the feedback outbox as a standalone entry, so the normal
     * background flush delivers it with no user action at all. An identical
     * report already waiting in the outbox is not queued twice (a crash
     * loop must not spam), and the store is cleared either way — whichever
     * path takes the report first owns it. Returns true when a stored
     * report was found (queued now, or already queued identically).
     * Callers run this off the main thread.
     */
    fun queueStoredReport(context: Context): Boolean {
        val report = peek(context) ?: return false
        val alreadyQueued = FeedbackOutbox.loadAll(context).any { it.crashLog == report }
        if (!alreadyQueued) {
            FeedbackOutbox.enqueue(
                context, "", "", null,
                crashLog = report, autoCrash = true
            )
        }
        clear(context, report)
        return true
    }

    /**
     * Drops the stored report once it has been queued with a feedback —
     * but only if the file still holds exactly that report, so a newer
     * crash written in the meantime is never deleted unseen.
     */
    fun clear(context: Context, expected: String) {
        try {
            val file = reportFile(context)
            if (file.exists() && file.readText() == expected) file.delete()
        } catch (e: Exception) {
            // A leftover report would just be offered again; harmless.
        }
    }

    /** Deletes the stored report outright (the user removed the attachment). */
    fun discard(context: Context) {
        try {
            reportFile(context).delete()
        } catch (e: Exception) {
            // Same as above: worst case it is offered once more.
        }
    }
}
