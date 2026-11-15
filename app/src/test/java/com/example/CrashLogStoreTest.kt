package com.example

import com.example.feedback.CrashLogStore
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the crash report contract: it is the app's own technical error
 * only — header (time, version, device, thread) plus the stack trace —
 * and it is capped so a pathological trace cannot bloat a feedback.
 * Plain JUnit: buildReport touches no Android APIs.
 */
class CrashLogStoreTest {

    @Test
    fun report_containsHeaderAndStackTrace() {
        val boom = IllegalStateException("kaboom")
        val report = CrashLogStore.buildReport(
            1_700_000_000_000L, "1.1.0", "Nokia 5.3", "main", boom
        )
        assertTrue(report.contains("appVersion: 1.1.0"))
        assertTrue(report.contains("device: Nokia 5.3"))
        assertTrue(report.contains("thread: main"))
        assertTrue(report.contains("java.lang.IllegalStateException: kaboom"))
        assertTrue(report.contains("CrashLogStoreTest"))
    }

    @Test
    fun report_respectsTheCap() {
        // A huge exception message makes the trace deterministically over
        // the cap, whatever the test runner's stack depth is.
        val boom = RuntimeException("x".repeat(20_000))
        val report = CrashLogStore.buildReport(0L, "1.1.0", "dev", "main", boom)
        assertTrue(report.length <= CrashLogStore.MAX_REPORT_CHARS + 32)
        assertTrue(report.contains("truncated"))
    }
}
