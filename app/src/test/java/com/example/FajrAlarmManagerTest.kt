package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.prayer.FajrAlarmManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FajrAlarmManagerTest {
    @Test
    fun getSettings_survivesIntStoredForLongTimestamp() {
        // Regression test for the 2026-11-11 restore crash: a JSON round-trip
        // stores fajr_alarm_next_timestamp as Int, and getSettings() must
        // still return 0L instead of throwing ClassCastException.
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("hamgam_prayer_prefs", Context.MODE_PRIVATE)
            .edit().putInt("fajr_alarm_next_timestamp", 0).apply()
        assertEquals(0L, FajrAlarmManager.getSettings(context).nextAlarmTimestamp)
    }
}
