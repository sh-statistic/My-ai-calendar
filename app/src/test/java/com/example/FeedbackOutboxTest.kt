package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.feedback.FeedbackOutbox
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the feedback outbox contract (Sadra's design, 2026-11-15): a tapped
 * Send queues the feedback on the device; delivery happens in the background,
 * oldest first, and an entry leaves the queue only after a confirmed
 * delivery — a failed attempt keeps it and everything behind it, in order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeedbackOutboxTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun cleanOutbox() {
        context.getSharedPreferences("feedback_outbox", Context.MODE_PRIVATE)
            .edit().clear().commit()
        File(context.filesDir, "feedback_outbox").deleteRecursively()
        File(context.filesDir, "crash_reports").deleteRecursively()
    }

    @Test
    fun codec_roundTripsItems() {
        val items = listOf(
            FeedbackOutbox.PendingFeedback("1", "سلام، نظر اول", "@a", "1.1.0", "Nokia 5.3", null),
            FeedbackOutbox.PendingFeedback("2", "second", "", "1.1.0", "Pixel", "2.jpg")
        )
        assertEquals(items, FeedbackOutbox.decodeItems(FeedbackOutbox.encodeItems(items)))
    }

    @Test
    fun codec_garbageDecodesToEmpty() {
        assertTrue(FeedbackOutbox.decodeItems(null).isEmpty())
        assertTrue(FeedbackOutbox.decodeItems("").isEmpty())
        assertTrue(FeedbackOutbox.decodeItems("not json").isEmpty())
        assertTrue(FeedbackOutbox.decodeItems("[{\"text\":\"no id\"}]").isEmpty())
    }

    @Test
    fun enqueue_keepsFifoOrder() {
        FeedbackOutbox.enqueue(context, "اول", "", null)
        FeedbackOutbox.enqueue(context, "دوم", "c", null)
        FeedbackOutbox.enqueue(context, "سوم", "", null)
        val loaded = FeedbackOutbox.loadAll(context)
        assertEquals(listOf("اول", "دوم", "سوم"), loaded.map { it.text })
        assertEquals("c", loaded[1].contact)
        assertEquals(3, FeedbackOutbox.pendingCount(context))
    }

    @Test
    fun enqueue_persistsPhotoUntilRemoved() {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val item = FeedbackOutbox.enqueue(context, "با عکس", "", bytes)
        assertTrue(FeedbackOutbox.photoBytes(context, item)!!.contentEquals(bytes))
        FeedbackOutbox.remove(context, item.id)
        assertNull(FeedbackOutbox.photoBytes(context, item))
        assertEquals(0, FeedbackOutbox.pendingCount(context))
    }

    @Test
    fun flush_deliversAllInOrderAndEmptiesQueue() = runBlocking {
        FeedbackOutbox.enqueue(context, "a", "", null)
        FeedbackOutbox.enqueue(context, "b", "", null)
        FeedbackOutbox.enqueue(context, "c", "", null)
        val delivered = mutableListOf<String>()
        val sent = FeedbackOutbox.flush(context) { item, _ ->
            delivered.add(item.text)
            true
        }
        assertEquals(3, sent)
        assertEquals(listOf("a", "b", "c"), delivered)
        assertEquals(0, FeedbackOutbox.pendingCount(context))
    }

    @Test
    fun flush_stopsAtFirstFailureKeepingTheRestInOrder() = runBlocking {
        FeedbackOutbox.enqueue(context, "a", "", null)
        FeedbackOutbox.enqueue(context, "b", "", null)
        FeedbackOutbox.enqueue(context, "c", "", null)
        val sent = FeedbackOutbox.flush(context) { item, _ -> item.text == "a" }
        assertEquals(1, sent)
        assertEquals(listOf("b", "c"), FeedbackOutbox.loadAll(context).map { it.text })
    }

    @Test
    fun flush_firstFailureKeepsEverything() = runBlocking {
        FeedbackOutbox.enqueue(context, "a", "", null)
        FeedbackOutbox.enqueue(context, "b", "", null)
        val sent = FeedbackOutbox.flush(context) { _, _ -> false }
        assertEquals(0, sent)
        assertEquals(2, FeedbackOutbox.pendingCount(context))
    }

    @Test
    fun enqueue_roundTripsCrashLog() {
        FeedbackOutbox.enqueue(context, "کرش کرد", "", null, crashLog = "TRACE-123")
        val loaded = FeedbackOutbox.loadAll(context)
        assertEquals(1, loaded.size)
        assertEquals("TRACE-123", loaded[0].crashLog)
    }

    @Test
    fun queueStoredReport_movesReportIntoOutboxOnce() {
        val dir = File(context.filesDir, "crash_reports").apply { mkdirs() }
        File(dir, "last_crash.txt").writeText("REPORT-BODY")
        assertTrue(com.example.feedback.CrashLogStore.queueStoredReport(context))
        val loaded = FeedbackOutbox.loadAll(context)
        assertEquals(1, loaded.size)
        assertTrue(loaded[0].autoCrash)
        assertEquals("REPORT-BODY", loaded[0].crashLog)
        assertEquals("", loaded[0].text)
        // Store consumed: a second call queues nothing new.
        assertTrue(!com.example.feedback.CrashLogStore.queueStoredReport(context))
        assertEquals(1, FeedbackOutbox.pendingCount(context))
    }

    @Test
    fun queueStoredReport_dedupesIdenticalQueuedReport() {
        // The same report already waits in the outbox (e.g. the user
        // attached it to a feedback): no second entry, store still cleared.
        FeedbackOutbox.enqueue(context, "کرش کرد", "", null, crashLog = "REPORT-BODY")
        val dir = File(context.filesDir, "crash_reports").apply { mkdirs() }
        File(dir, "last_crash.txt").writeText("REPORT-BODY")
        assertTrue(com.example.feedback.CrashLogStore.queueStoredReport(context))
        assertEquals(1, FeedbackOutbox.pendingCount(context))
        assertTrue(!File(dir, "last_crash.txt").exists())
    }
}
