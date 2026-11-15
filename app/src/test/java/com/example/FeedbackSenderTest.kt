package com.example

import com.example.feedback.FeedbackSender
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the feedback payload contract the relay (Cloudflare Worker) expects:
 * fields present, text/contact trimmed and capped, photo only when attached.
 * Robolectric for the real org.json, same as BackupArchiveTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FeedbackSenderTest {

    @Test
    fun payload_hasAllFields_andTrimsText() {
        val json = JSONObject(
            FeedbackSender.buildPayloadJson("  سلام، برنامه عالیه  ", " @sadra ", "1.1.0", "Nokia 5.3", null)
        )
        assertEquals("سلام، برنامه عالیه", json.getString("text"))
        assertEquals("@sadra", json.getString("contact"))
        assertEquals("1.1.0", json.getString("appVersion"))
        assertEquals("Nokia 5.3", json.getString("device"))
        assertFalse(json.has("photoBase64"))
    }

    @Test
    fun payload_capsLongTextAndContact() {
        val json = JSONObject(
            FeedbackSender.buildPayloadJson(
                "x".repeat(FeedbackSender.MAX_TEXT_LENGTH + 500),
                "y".repeat(FeedbackSender.MAX_CONTACT_LENGTH + 50),
                "1.1.0", "dev", "QUJD"
            )
        )
        assertEquals(FeedbackSender.MAX_TEXT_LENGTH, json.getString("text").length)
        assertEquals(FeedbackSender.MAX_CONTACT_LENGTH, json.getString("contact").length)
        assertTrue(json.has("photoBase64"))
        assertEquals("QUJD", json.getString("photoBase64"))
        assertFalse(json.has("crashLog"))
    }

    @Test
    fun payload_includesCrashLogWhenProvided() {
        val json = JSONObject(
            FeedbackSender.buildPayloadJson(
                "برنامه بسته شد", "", "1.1.0", "dev", null, "STACK TRACE HERE"
            )
        )
        assertEquals("STACK TRACE HERE", json.getString("crashLog"))
        assertFalse(json.has("autoCrash"))
    }

    @Test
    fun payload_marksAutomaticCrashReports() {
        val json = JSONObject(
            FeedbackSender.buildPayloadJson(
                "", "", "1.1.0", "dev", null, "TRACE", autoCrash = true
            )
        )
        assertTrue(json.getBoolean("autoCrash"))
        assertEquals("TRACE", json.getString("crashLog"))
    }

    @Test
    fun payload_carriesClientIdForRelayDedupe() {
        val withId = JSONObject(
            FeedbackSender.buildPayloadJson(
                "سلام", "", "1.0.4", "dev", null, clientId = "entry-123"
            )
        )
        assertEquals("entry-123", withId.getString("clientId"))
        val withoutId = JSONObject(
            FeedbackSender.buildPayloadJson("سلام", "", "1.0.4", "dev", null)
        )
        assertFalse(withoutId.has("clientId"))
    }
}
