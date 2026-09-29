package com.example

import com.example.calendar.PersianCalendarHelper
import com.example.data.EventEntity
import com.example.data.NoteEntity
import com.example.data.SyncData
import com.example.data.TaskEntity
import com.example.prayer.PrayerTimesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPersianCalendarConversionRoundTrip() {
        // Test multiple dates roundtrip
        val testDates = listOf(
            Triple(2026, 3, 21),
            Triple(2026, 1, 1),
            Triple(2025, 12, 31),
            Triple(2024, 3, 20)
        )

        for ((gy, gm, gd) in testDates) {
            val jDate = PersianCalendarHelper.gregorianToJalali(gy, gm, gd)
            assertTrue(jDate.year in 1400..1410)
            assertTrue(jDate.month in 1..12)
            assertTrue(jDate.day in 1..31)

            val backG = PersianCalendarHelper.jalaliToGregorian(jDate.year, jDate.month, jDate.day)
            assertEquals(gy, backG.year)
            assertEquals(gm, backG.month)
            assertEquals(gd, backG.day)
        }
    }

    @Test
    fun testPersianDigitsFormatting() {
        val formatted = PersianCalendarHelper.toPersianDigits("1405/01/15 - 10:30")
        assertEquals("۱۴۰۵/۰۱/۱۵ - ۱۰:۳۰", formatted)
    }

    @Test
    fun testTehranPrayerTimesCalculation() {
        val tehran = PrayerTimesCalculator.getCityByName("تهران")
        assertEquals("تهران", tehran.nameFa)

        // Calculate for March 21, 2026 (Nowruz)
        val times = PrayerTimesCalculator.calculatePrayerTimes(
            tehran.latitude, tehran.longitude, 2026, 3, 21
        )

        assertNotNull(times.fajr)
        assertNotNull(times.sunrise)
        assertNotNull(times.dhuhr)
        assertNotNull(times.sunset)
        assertNotNull(times.maghrib)
        assertNotNull(times.midnight)

        // Fajr should be before sunrise
        assertTrue(times.fajrHour < times.sunriseHour)
        // Sunrise should be before dhuhr
        assertTrue(times.sunriseHour < times.dhuhrHour)
        // Dhuhr should be before sunset
        assertTrue(times.dhuhrHour < times.sunsetHour)
        // Sunset should be before maghrib
        assertTrue(times.sunsetHour < times.maghribHour)
    }

    @Test
    fun testSyncDataJsonSerialization() {
        val event = EventEntity(
            title = "جلسه تست",
            description = "تست همگام‌سازی",
            persianDate = "1405/01/01",
            gregorianDate = "2026-03-21"
        )
        val task = TaskEntity(
            title = "کار مهم",
            isCompleted = false,
            persianDueDate = "1405/01/05"
        )
        val note = NoteEntity(
            title = "یادداشت تست",
            content = "محتوای تست"
        )
        val syncData = SyncData(
            deviceId = "Test-Device",
            timestamp = 123456789L,
            events = listOf(event),
            tasks = listOf(task),
            notes = listOf(note)
        )

        val jsonStr = syncData.toJsonString()
        assertTrue(jsonStr.contains("جلسه تست"))
        assertTrue(jsonStr.contains("کار مهم"))
        assertTrue(jsonStr.contains("یادداشت تست"))

        val restored = SyncData.fromJsonString(jsonStr)
        assertEquals(1, restored.events.size)
        assertEquals("جلسه تست", restored.events[0].title)
        assertEquals(1, restored.tasks.size)
        assertEquals("کار مهم", restored.tasks[0].title)
        assertEquals(1, restored.notes.size)
        assertEquals("یادداشت تست", restored.notes[0].title)
    }
}
